const WebSocket = require('ws');
const {transcribe} = require('./speech');

function resample16k(pcm, fromRate=24000, toRate=16000) {
  if (fromRate===toRate) return pcm;
  const input=new Int16Array(pcm.buffer,pcm.byteOffset,Math.floor(pcm.length/2));
  const n=Math.max(1,Math.floor(input.length*toRate/fromRate)); const out=Buffer.alloc(n*2);
  for(let i=0;i<n;i++){const p=i*fromRate/toRate, a=Math.floor(p), f=p-a; const x=input[Math.min(a,input.length-1)], y=input[Math.min(a+1,input.length-1)]; out.writeInt16LE(Math.round(x+(y-x)*f),i*2)}
  return out;
}
function createRealtimeProvider({apiKey=process.env.OPENAI_API_KEY,model=process.env.OPENAI_REALTIME_MODEL||'gpt-realtime',wsFactory=(url,opts)=>new WebSocket(url,opts), transcribeAudio=transcribe}={}) {
  if(!apiKey) throw Error('OPENAI_API_KEY is required for Realtime mode');
  function submit({id,pcm}) {
    const controller=new AbortController(); const job={id,status:'connecting'};
    const promise=new Promise((resolve,reject)=>{
      let socket, audio=[], caption=''; let settled=false;
      const fail=e=>{if(settled)return;settled=true;console.log(JSON.stringify({event:'realtime_error',turnId:id,message:e.message}));try{socket?.close()}catch{} reject(e)};
      controller.signal.addEventListener('abort',()=>fail(Error('Turn canceled')),{once:true});
      (async()=>{
        try {
          job.status='transcribing'; const localCaption=await transcribeAudio(pcm,controller.signal); controller.signal.throwIfAborted();
          const inputPcm=resample16k(pcm,16000,24000);
          job.status='connecting'; socket=wsFactory(`wss://api.openai.com/v1/realtime?model=${encodeURIComponent(model)}`,{headers:{Authorization:`Bearer ${apiKey}`} });
          socket.on('open',()=>{job.status='sending'; socket.send(JSON.stringify({type:'session.update',session:{type:'realtime',output_modalities:['audio'],instructions:process.env.STEVE_SYSTEM_PROMPT||'You are Steve, a calm, clever, kind voice companion on an Echo Show 5. Keep replies concise.',audio:{input:{format:{type:'audio/pcm',rate:24000}},output:{format:{type:'audio/pcm',rate:24000}}}}})); socket.send(JSON.stringify({type:'input_audio_buffer.append',audio:inputPcm.toString('base64')})); socket.send(JSON.stringify({type:'input_audio_buffer.commit'})); socket.send(JSON.stringify({type:'response.create'})); job.status='waiting';});
          socket.on('message',raw=>{let e;try{e=JSON.parse(raw.toString())}catch{return} if(e.type==='response.output_audio.delta'||e.type==='response.audio.delta')audio.push(Buffer.from(e.delta,'base64')); if(e.type==='response.output_audio_transcript.delta'||e.type==='response.audio_transcript.delta')caption+=e.delta||''; if((e.type==='response.output_audio_transcript.done'||e.type==='response.audio_transcript.done')&&e.transcript)caption=e.transcript; if(e.type==='error')fail(Error(e.error?.message||'Realtime API error')); if(e.type==='response.done'){if(!caption.trim())caption=localCaption; const out=resample16k(Buffer.concat(audio),24000); if(!out.length) return fail(Error('Realtime returned no audio')); settled=true;socket.close();resolve({caption:caption.trim().slice(0,800),pcm:out,source:'openai_realtime'});}});
          socket.on('error',fail); socket.on('close',()=>{if(!settled)fail(Error('Realtime connection closed before response'))});
        } catch(e){fail(e)}
      })();
    });
    promise.catch(()=>{}); return {get status(){return job.status},promise,cancel:()=>controller.abort()};
  }
  return {submit};
}
module.exports={createRealtimeProvider,resample16k};
