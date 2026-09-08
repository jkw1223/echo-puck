const {transcribe,speak}=require('./speech');
const http=require('node:http');
const fs=require('node:fs');
const path=require('node:path');
const {randomBytes,timingSafeEqual}=require('node:crypto');

function createBrowserQueue({transcribeAudio=transcribe,synthesize=speak}={}){
 const jobs=new Map();
 function submit({id,pcm}){
  if(jobs.size)throw Error('Browser bridge is busy with another turn');
  const controller=new AbortController();
  const job={id,status:'transcribing',createdAt:Date.now(),transcript:null,controller,resolveReply:null};
  jobs.set(id,job);
  const promise=(async()=>{
   try{
    job.transcript=await transcribeAudio(pcm,controller.signal);controller.signal.throwIfAborted();
    job.status='awaiting_browser';
    console.log(JSON.stringify({event:'browser_turn_ready',turnId:id,transcriptChars:job.transcript.length}));
    const text=await new Promise((resolve,reject)=>{
     if(controller.signal.aborted){reject(Error('Turn canceled'));return;}
     job.resolveReply=resolve;
     controller.signal.addEventListener('abort',()=>reject(Error('Turn canceled')),{once:true});
    });
    controller.signal.throwIfAborted();job.status='synthesizing';
    const replyPcm=await synthesize(text,controller.signal);controller.signal.throwIfAborted();
    console.log(JSON.stringify({event:'browser_reply_ready',turnId:id,replyChars:text.length,replyBytes:replyPcm.length,conversationUrl:job.conversationUrl}));
    return {caption:text,pcm:replyPcm,source:'chatgpt_web'};
   }finally{jobs.delete(id);}
  })();
  return {get status(){return job.status;},promise,cancel:()=>controller.abort()};
 }
 function list(){return [...jobs.values()].map(({id,status,createdAt,transcript,conversationUrl})=>({id,status,createdAt,transcript,conversationUrl}));}
 function claim(id,conversationUrl){
  const job=jobs.get(id);
  if(!job||job.status!=='awaiting_browser')throw Error('Turn unavailable or already claimed; do not resubmit');
  if(typeof conversationUrl!=='string'||!/^https:\/\/chatgpt\.com\/c\/[0-9a-f-]+$/.test(conversationUrl))throw Error('Expected a verified ChatGPT conversation URL');
  job.conversationUrl=conversationUrl;job.status='browser_claimed';return {id,status:job.status,transcript:job.transcript};
 }
 function reply(id,text){
  const job=jobs.get(id);
  if(!job||job.status!=='browser_claimed')throw Error('Turn unavailable or unclaimed');
  if(typeof text!=='string'||!text.trim()||text.length>800)throw Error('Reply must contain 1–800 characters');
  job.status='synthesizing';job.resolveReply(text.trim());return {id,status:job.status};
 }
 function cancelAll(){for(const j of jobs.values())j.controller.abort();}
 return {submit,list,claim,reply,cancelAll};
}
function startOperator(queue,{port=8790,stateDir=path.join(__dirname,'.runtime')}={}){
 fs.mkdirSync(stateDir,{recursive:true,mode:0o700});
 const key=randomBytes(32).toString('hex');const keyPath=path.join(stateDir,'operator-token');
 const send=(res,status,value)=>{res.writeHead(status,{'content-type':'application/json','cache-control':'no-store'});res.end(JSON.stringify(value));};
 const server=http.createServer((req,res)=>{
  const supplied=Buffer.from((req.headers.authorization||'').replace(/^Bearer /,''));
  const expected=Buffer.from(key);
  if(req.headers.origin||supplied.length!==expected.length||!timingSafeEqual(supplied,expected)){send(res,403,{error:'Operator authorization required'});req.resume();return;}
  if(req.method==='GET'&&req.url==='/jobs'){send(res,200,{jobs:queue.list()});return;}
  const match=/^\/jobs\/([0-9a-f-]+)\/(claim|reply)$/.exec(req.url);
  if(req.method!=='POST'||!match){send(res,404,{error:'Not found'});req.resume();return;}
  let text='';req.setTimeout(5000,()=>req.destroy());
  req.on('data',chunk=>{text+=chunk;if(Buffer.byteLength(text)>8192)req.destroy();});
  req.on('error',()=>{});
  req.on('end',()=>{try{
   const data=JSON.parse(text);
   send(res,200,match[2]==='claim'?queue.claim(match[1],data.conversationUrl):queue.reply(match[1],data.text));
  }catch(e){send(res,409,{error:e.message});}});
 });
 server.listen(port,'127.0.0.1',()=>{fs.writeFileSync(keyPath,key,{mode:0o600});console.log(`Puck operator queue listening on 127.0.0.1:${server.address().port}`);});
 return {server,close:()=>{queue.cancelAll();server.close();if(fs.existsSync(keyPath)&&fs.readFileSync(keyPath,'utf8')===key)fs.rmSync(keyPath,{force:true});}};
}
module.exports={createBrowserQueue,startOperator};
