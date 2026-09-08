const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {execFile} = require('node:child_process');
const {promisify} = require('node:util');
const execute = promisify(execFile);
const WHISPER = process.env.PUCK_WHISPER_BIN || '/Users/jason/Documents/Whisper/whisper.cpp/build/bin/whisper-cli';
const MODEL = process.env.PUCK_WHISPER_MODEL || '/Users/jason/Documents/Whisper/whisper.cpp/models/ggml-base.en.bin';
function wav(pcm) {
 const h=Buffer.alloc(44);h.write('RIFF');h.writeUInt32LE(36+pcm.length,4);h.write('WAVEfmt ',8);
 h.writeUInt32LE(16,16);h.writeUInt16LE(1,20);h.writeUInt16LE(1,22);h.writeUInt32LE(16000,24);
 h.writeUInt32LE(32000,28);h.writeUInt16LE(2,32);h.writeUInt16LE(16,34);h.write('data',36);h.writeUInt32LE(pcm.length,40);
 return Buffer.concat([h,pcm]);
}
function pcmFromWav(buffer) {
 if(buffer.toString('ascii',0,4)!=='RIFF'||buffer.toString('ascii',8,12)!=='WAVE')throw Error('Invalid speech WAV');
 let format=false,data=null;
 for(let at=12;at+8<=buffer.length;){
  const type=buffer.toString('ascii',at,at+4),size=buffer.readUInt32LE(at+4),begin=at+8;
  if(begin+size>buffer.length)throw Error('Truncated speech WAV');
  if(type==='fmt '){
   if(size<16 || buffer.readUInt16LE(begin)!==1 || buffer.readUInt16LE(begin+2)!==1 || buffer.readUInt32LE(begin+4)!==16000 || buffer.readUInt16LE(begin+14)!==16)throw Error('Unexpected speech format');
   format=true;
  }
  if(type==='data')data=buffer.subarray(begin,begin+size);
  at=begin+size+(size%2);
 }
 if(!format||!data?.length||data.length%2)throw Error('Missing speech PCM');
 return data;
}
async function inTemp(prefix, work) {
 const dir=await fs.mkdtemp(path.join(os.tmpdir(),prefix));
 try{return await work(dir);}finally{await fs.rm(dir,{recursive:true,force:true});}
}
async function transcribe(pcm,signal){
 return inTemp('puck-transcribe-',async dir=>{
  const input=path.join(dir,'input.wav'),output=path.join(dir,'transcript');
  await fs.writeFile(input,wav(pcm),{mode:0o600});
  await execute(WHISPER,['-m',MODEL,'-f',input,'-otxt','-of',output,'-ng','-t','4'],{signal,timeout:60000,maxBuffer:1024*1024});
  const text=(await fs.readFile(output+'.txt','utf8')).replace(/\s+/g,' ').trim();
  if(!text||text.length>4000||/^\[.*\]$/.test(text))throw Error('No usable speech transcript');
  return text;
 });
}
async function speak(text,signal){
 return inTemp('puck-speech-',async dir=>{
  const input=path.join(dir,'reply.txt'),aiff=path.join(dir,'reply.aiff'),output=path.join(dir,'reply.wav');
  await fs.writeFile(input,text,{mode:0o600});
  await execute('/usr/bin/say',['-f',input,'-o',aiff],{signal,timeout:30000});
  await execute('/usr/bin/afconvert',['-f','WAVE','-d','LEI16@16000','-c','1',aiff,output],{signal,timeout:15000});
  const pcm=pcmFromWav(await fs.readFile(output));
  if(pcm.length>960000)throw Error('Spoken reply exceeds 30 seconds; supply a shorter response');
  return pcm;
 });
}
module.exports={transcribe,speak,wav,pcmFromWav};
