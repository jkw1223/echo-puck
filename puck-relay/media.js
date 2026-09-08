const { randomBytes, createHash } = require('node:crypto');
const MAX_PCM_BYTES = 16000 * 2 * 30;
const CAPTION = 'Jason, your recording reached the Mac. This is the Puck bridge test reply.';
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function createMedia({ replyPcm, turnTtlMs = 60000, now = Date.now, provider = null }) {
  if (!Buffer.isBuffer(replyPcm) || !replyPcm.length || replyPcm.length > MAX_PCM_BYTES || replyPcm.length % 2) throw Error('Invalid reply PCM fixture');
  const connections = new Map();
  const json = (res, status, value) => {
    if (res.destroyed || res.writableEnded) return;
    const body = JSON.stringify(value);
    res.writeHead(status, {'content-type':'application/json', 'content-length':Buffer.byteLength(body), 'cache-control':'no-store'});
    res.end(body);
  };
  function drop(token) {
    const state = connections.get(token);
    state?.active?.job?.cancel();
    if (state?.active?.request) state.active.request.destroy();
    if (state) state.active = null;
    connections.delete(token);
  }
  function register(isLive = () => true) {
    const token = randomBytes(32).toString('hex');
    connections.set(token, { session:null, active:null, seen:new Set(), isLive });
    return token;
  }
  function identify(token, session) { const state=connections.get(token); if (state) state.session=session; }
  function sweep() {
    for (const state of connections.values()) {
      if (state.active && now() - state.active.startedAt > turnTtlMs) {
        state.active.job?.cancel(); state.active.request?.destroy(); state.active = null;
      }
    }
  }
  function handle(req, res) {
    if (!req.url.startsWith('/turns/')) return false;
    const token = (req.headers.authorization || '').replace(/^Bearer /, '');
    const state = connections.get(token);
    if (!state?.session || !state.isLive() || req.headers['x-puck-session'] !== state.session) {
      json(res, 401, {error:'Live device connection required'}); req.resume(); return true;
    }
    const match = /^\/turns\/([^/]+)(\/reply)?$/.exec(req.url);
    if (!match || !UUID.test(match[1])) { json(res,400,{error:'Invalid turn ID'});req.resume();return true; }
    const id=match[1];
    const valid = turn => connections.get(token) === state && state.isLive() && state.active === turn && now()-turn.startedAt <= turnTtlMs;
    if (req.method === 'DELETE' && !match[2]) {
      if (state.active?.id === id) { const old=state.active;state.active=null;old.job?.cancel();old.request?.destroy(); }
      // Remember cancellation even if DELETE overtakes the upload on another HTTP connection.
      if (state.seen.size >= 512 && !state.seen.has(id)) { json(res,429,{error:'Reconnect to start more turns'});return true; }
      state.seen.add(id); json(res,200,{type:'turn_cancelled',turnId:id}); return true;
    }
    if (req.method === 'POST' && !match[2]) {
      const size = Number(req.headers['content-length']);
      if (req.headers['content-type'] !== 'application/octet-stream' || req.headers['x-puck-format'] !== 'pcm_s16le_16000_mono' ||
          !Number.isSafeInteger(size) || size < 3200 || size > MAX_PCM_BYTES || size % 2) {
        json(res,400,{error:'Expected 0.1–30 seconds PCM16 mono at 16 kHz'});req.resume();return true;
      }
      if (state.active || state.seen.has(id) || state.seen.size >= 512) {
        json(res,409,{error:'Turn active, repeated, or session full'});req.resume();return true;
      }
      const turn={id, startedAt:now(), request:req, accepted:false};
      state.active=turn;state.seen.add(id);
      const chunks=[];let bytes=0;
      req.setTimeout(15000, () => { if(state.active===turn)state.active=null;req.destroy(); });
      req.on('data', chunk => {
        bytes+=chunk.length;
        if (!valid(turn) || bytes>size || bytes>MAX_PCM_BYTES) { if(state.active===turn)state.active=null;req.destroy();return; }
        chunks.push(chunk);
      });
      req.on('error', () => { if(state.active===turn)state.active=null; });
      req.on('aborted', () => { if(state.active===turn)state.active=null; });
      req.on('end', () => {
        if(!valid(turn)){json(res,410,{error:'Turn expired or canceled'});return;}
        if(bytes!==size){state.active=null;json(res,400,{error:'Incomplete audio'});return;}
        const pcm=Buffer.concat(chunks);
        const sha256=createHash('sha256').update(pcm).digest('hex');
        turn.request=null;turn.accepted=true;
        if(provider){
          try{
            turn.job=provider.submit({id,pcm});
            turn.job.promise.then(result=>{
              if(!valid(turn))return;
              if(typeof result.caption!=='string'||!result.caption.trim()||result.caption.length>1000||
                  !Buffer.isBuffer(result.pcm)||!result.pcm.length||result.pcm.length>MAX_PCM_BYTES||result.pcm.length%2||!['chatgpt_web','openai','openai_realtime'].includes(result.source))throw Error('Invalid provider reply');
              turn.result=result;
            }).catch(error=>{if(valid(turn))turn.error=error.message;});
          }catch(error){turn.error=error.message;}
        }
        console.log(JSON.stringify({event:'turn_received',turnId:id,bytes,durationMs:bytes/32,sha256}));
        json(res,202,{type:'turn_received',turnId:id,receivedBytes:bytes,sha256});
      });
      return true;
    }
    if(req.method==='GET' && match[2]) {
      const turn=state.active;
      if(!turn || turn.id!==id || !valid(turn)){json(res,410,{error:'Turn unavailable'});return true;}
      if(!turn.accepted){json(res,409,{error:'Upload incomplete'});return true;}
      if(turn.error){state.active=null;json(res,502,{error:'Conversation bridge failed: '+turn.error});return true;}
      if(provider&&!turn.result){json(res,202,{type:'turn_pending',turnId:id,status:turn.job?.status||'preparing',retryAfterMs:1000});return true;}
      const result=turn.result||{caption:CAPTION,pcm:replyPcm,source:'local_fixture'};
      // Consume once. A lost response fails the turn; reconnect never replays audio.
      state.active=null;
      json(res,200,{type:'turn_reply',turnId:id,source:result.source,caption:result.caption,
        format:'pcm_s16le_16000_mono',pcmBase64:result.pcm.toString('base64')});
      console.log(JSON.stringify({event:'turn_reply_sent',turnId:id,bytes:result.pcm.length,source:result.source}));
      return true;
    }
    json(res,405,{error:'Unsupported method'});req.resume();return true;
  }
  return {register,identify,drop,sweep,handle};
}
module.exports={createMedia,MAX_PCM_BYTES,CAPTION};
