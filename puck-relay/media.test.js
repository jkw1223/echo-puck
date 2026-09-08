const {test}=require('node:test');
const assert=require('node:assert/strict');
const {once}=require('node:events');
const {randomUUID,createHash}=require('node:crypto');
const http=require('node:http');
const {WebSocket}=require('ws');
const {createRelay}=require('./index');
const PCM=Buffer.alloc(6400,17);
async function setup(t){
 const relay=createRelay({replyPcm:PCM});relay.server.listen(0,'127.0.0.1');await once(relay.server,'listening');
 t.after(()=>relay.close());return {...relay,base:`http://127.0.0.1:${relay.server.address().port}`};
}
async function device(base){
 const ws=new WebSocket(base.replace('http:','ws:')+'/puck');const greeting=once(ws,'message');await once(ws,'open');
 const token=JSON.parse((await greeting)[0]).mediaToken;const session=randomUUID();
 const ack=once(ws,'message');ws.send(JSON.stringify({type:'heartbeat',protocol:1,id:1,session,deviceId:'fixture',device:'checkers',model:'Echo Show 5',android:'11'}));await ack;
 const headers={Authorization:`Bearer ${token}`,'X-Puck-Session':session};
 async function send(id,method='POST',body=PCM,extra={}){
  return fetch(`${base}/turns/${id}`,{method,headers:{...headers,...(method==='POST'?{'Content-Type':'application/octet-stream','X-Puck-Format':'pcm_s16le_16000_mono'}:{}),...extra},...(method==='POST'?{body}:{})});
 }
 return {ws,headers,send};
}
test('media receipt matches uploaded bytes; reply is consumed exactly once',async t=>{
 const {base}=await setup(t);const d=await device(base);const id=randomUUID();
 const res=await d.send(id);assert.equal(res.status,202);const receipt=await res.json();
 assert.equal(receipt.receivedBytes,PCM.length);assert.equal(receipt.sha256,createHash('sha256').update(PCM).digest('hex'));
 const reply=await (await d.send(id+'/reply','GET')).json();assert.equal(reply.turnId,id);assert.equal(reply.source,'local_fixture');assert.deepEqual(Buffer.from(reply.pcmBase64,'base64'),PCM);
 assert.equal((await d.send(id+'/reply','GET')).status,410);
 assert.equal((await d.send(id)).status,409);
});
test('media requires the live connection ticket and matching session',async t=>{
 const {base}=await setup(t);const d=await device(base);const id=randomUUID();
 assert.equal((await d.send(id,'POST',PCM,{Authorization:'Bearer invalid'})).status,401);
 assert.equal((await d.send(id,'POST',PCM,{'X-Puck-Session':'wrong'})).status,401);
 const closed=once(d.ws,'close');d.ws.close();await closed;
 assert.equal((await d.send(id)).status,401);
});
test('cancel withdraws pending reply and prevents late upload resurrection',async t=>{
 const {base}=await setup(t);const d=await device(base);const id=randomUUID();
 assert.equal((await d.send(id)).status,202);assert.equal((await d.send(id,'DELETE')).status,200);
 assert.equal((await d.send(id+'/reply','GET')).status,410);
 const early=randomUUID();assert.equal((await d.send(early,'DELETE')).status,200);assert.equal((await d.send(early)).status,409);
 assert.equal((await d.send(randomUUID())).status,202);
});
test('rejects unsupported media and concurrent turns without losing the accepted turn',async t=>{
 const {base}=await setup(t);const d=await device(base);
 assert.equal((await d.send(randomUUID(),'POST',Buffer.alloc(960002))).status,400);
 assert.equal((await d.send(randomUUID(),'POST',Buffer.alloc(3199))).status,400);
 assert.equal((await d.send(randomUUID(),'POST',PCM,{'X-Puck-Format':'mp3'})).status,400);
 const id=randomUUID();assert.equal((await d.send(id)).status,202);assert.equal((await d.send(randomUUID())).status,409);
 assert.equal((await d.send(id+'/reply','GET')).status,200);
});
test('connection replacement cannot retrieve old reply',async t=>{
 const {base}=await setup(t);const d=await device(base);const id=randomUUID();await d.send(id);
 const closed=once(d.ws,'close');d.ws.close();await closed;
 const next=await device(base);assert.equal((await next.send(id+'/reply','GET')).status,410);
 assert.equal((await d.send(id+'/reply','GET')).status,401);
});
test('cancel aborts an incomplete upload and permits a new turn',async t=>{
 const {base}=await setup(t);const d=await device(base);const id=randomUUID();
 const req=http.request(base+'/turns/'+id,{method:'POST',headers:{...d.headers,'Content-Type':'application/octet-stream','X-Puck-Format':'pcm_s16le_16000_mono','Content-Length':'6400'}});
 req.on('error',()=>{});const closed=new Promise(resolve=>req.once('close',resolve));req.write(PCM.subarray(0,3200));
 await new Promise(resolve=>setTimeout(resolve,30));
 assert.equal((await d.send(id,'DELETE')).status,200);await closed;
 assert.equal((await d.send(id+'/reply','GET')).status,410);assert.equal((await d.send(randomUUID())).status,202);
});
