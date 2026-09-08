const {test}=require('node:test');
const assert=require('node:assert/strict');
const {randomUUID}=require('node:crypto');
const {once}=require('node:events');
const {createBrowserQueue}=require('./browser-queue');
const {createRelay}=require('./index');
const {WebSocket}=require('ws');
const {wav,pcmFromWav}=require('./speech');
const PCM=Buffer.alloc(6400,4),URL='https://chatgpt.com/c/6a9c74ae-5908-83ea-ab3f-b8abfca171b1';
const tick=()=>new Promise(resolve=>setImmediate(resolve));
test('supervised job requires a single claim and preserves the returned text',async()=>{
 let spoken;const q=createBrowserQueue({transcribeAudio:async()=>"What did we just build?",synthesize:async text=>{spoken=text;return PCM;}});
 const id=randomUUID(),job=q.submit({id,pcm:PCM});await tick();
 assert.equal(q.list()[0].status,'awaiting_browser');
 assert.throws(()=>q.reply(id,'early'),/unclaimed/);
 assert.throws(()=>q.claim(id,'https://example.com/c/123'),/verified ChatGPT/);
 assert.equal(q.claim(id,URL).transcript,'What did we just build?');
 assert.throws(()=>q.claim(id,URL),/already claimed/);
 const answer='We built the Puck media bridge, Jason.';q.reply(id,answer);
 assert.deepEqual(await job.promise,{caption:answer,pcm:PCM,source:'chatgpt_web'});
 assert.equal(spoken,answer);assert.equal(q.list().length,0);
});
test('canceling a claimed browser turn prevents a late answer or synthesis',async()=>{
 let spoken=false;const q=createBrowserQueue({transcribeAudio:async()=>"test",synthesize:async()=>{spoken=true;return PCM;}});
 const id=randomUUID(),job=q.submit({id,pcm:PCM});const rejected=assert.rejects(job.promise,/canceled/);
 await tick();q.claim(id,URL);job.cancel();await rejected;
 assert.throws(()=>q.reply(id,'late answer'),/unavailable/);assert.equal(spoken,false);assert.equal(q.list().length,0);
});
test('only one browser job may be in flight',async()=>{
 const q=createBrowserQueue({transcribeAudio:async()=>"test",synthesize:async()=>PCM});
 const job=q.submit({id:randomUUID(),pcm:PCM});const rejected=assert.rejects(job.promise);
 assert.throws(()=>q.submit({id:randomUUID(),pcm:PCM}),/busy/);
 q.cancelAll();await rejected;
});
test('PCM WAV conversion validates the speech format',()=>{
 assert.deepEqual(pcmFromWav(wav(PCM)),PCM);
 const broken=wav(PCM);broken.writeUInt32LE(48000,24);assert.throws(()=>pcmFromWav(broken),/format/);
 assert.throws(()=>pcmFromWav(wav(PCM).subarray(0,48)),/Truncated/);
});
async function connected(t,provider){
 const relay=createRelay({replyPcm:PCM,provider});relay.server.listen(0,'127.0.0.1');await once(relay.server,'listening');t.after(()=>relay.close());
 const base=`http://127.0.0.1:${relay.server.address().port}`;
 const ws=new WebSocket(base.replace('http:','ws:')+'/puck'),hello=once(ws,'message');await once(ws,'open');const token=JSON.parse((await hello)[0]).mediaToken;
 const session=randomUUID(),ack=once(ws,'message');ws.send(JSON.stringify({type:'heartbeat',protocol:1,id:1,session,deviceId:'test',device:'checkers',model:'Echo Show 5',android:'11'}));await ack;
 const headers={Authorization:`Bearer ${token}`,'X-Puck-Session':session};
 const request=(id,method)=>fetch(base+'/turns/'+id,{method,headers:{...headers,'Content-Type':'application/octet-stream','X-Puck-Format':'pcm_s16le_16000_mono'},...(method==='POST'?{body:PCM}:{})});
 return {request,ws};
}
test('HTTP polls pending then returns actual provider reply once',async t=>{
 let resolve;const provider={submit:()=>({status:'awaiting_browser',promise:new Promise(r=>resolve=r),cancel(){}})};
 const {request}=await connected(t,provider),id=randomUUID();assert.equal((await request(id,'POST')).status,202);
 const pending=await request(id+'/reply','GET');assert.equal(pending.status,202);assert.equal((await pending.json()).status,'awaiting_browser');
 resolve({caption:'Actual browser response',pcm:PCM,source:'chatgpt_web'});await tick();
 const result=await (await request(id+'/reply','GET')).json();assert.equal(result.source,'chatgpt_web');assert.equal(result.caption,'Actual browser response');
 assert.equal((await request(id+'/reply','GET')).status,410);
});
test('canceling HTTP turn aborts provider and rejects late result',async t=>{
 let resolve,canceled=false;const provider={submit:()=>({status:'browser_claimed',promise:new Promise(r=>resolve=r),cancel(){canceled=true;}})};
 const {request}=await connected(t,provider),id=randomUUID();await request(id,'POST');await request(id,'DELETE');assert.equal(canceled,true);
 resolve({caption:'Late answer',pcm:PCM,source:'chatgpt_web'});await tick();assert.equal((await request(id+'/reply','GET')).status,410);
});
