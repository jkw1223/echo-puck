const {transcribe, speak} = require('./speech');

function createOpenAIProvider({apiKey=process.env.OPENAI_API_KEY, model=process.env.OPENAI_MODEL||'gpt-4o-mini', transcribeAudio=transcribe, synthesize=speak, fetchImpl=globalThis.fetch}={}) {
  if (!apiKey) throw Error('OPENAI_API_KEY is required for direct OpenAI mode');
  if (typeof fetchImpl !== 'function') throw Error('Node fetch is required for direct OpenAI mode');
  const system = process.env.STEVE_SYSTEM_PROMPT || 'You are Steve, a calm, clever, kind voice companion on an Echo Show 5. Answer clearly and conversationally. Keep spoken replies concise, usually under 120 words.';
  function submit({id,pcm}) {
    const controller = new AbortController();
    const job = {id, status:'transcribing', controller};
    const promise = (async()=>{
      const transcript = await transcribeAudio(pcm, controller.signal);
      controller.signal.throwIfAborted(); job.status='calling_openai';
      const response = await fetchImpl('https://api.openai.com/v1/responses', {
        method:'POST', signal:controller.signal,
        headers:{'authorization':`Bearer ${apiKey}`,'content-type':'application/json'},
        body:JSON.stringify({model, input:[
          {role:'system',content:[{type:'input_text',text:system}]},
          {role:'user',content:[{type:'input_text',text:transcript}]}
        ], max_output_tokens:300})
      });
      if (!response.ok) throw Error(`OpenAI request failed (${response.status})`);
      const data = await response.json();
      const text = typeof data.output_text==='string' ? data.output_text.trim() : '';
      if (!text || text.length>800) throw Error('OpenAI returned no usable reply');
      controller.signal.throwIfAborted(); job.status='synthesizing';
      const replyPcm = await synthesize(text, controller.signal);
      controller.signal.throwIfAborted(); job.status='complete';
      console.log(JSON.stringify({event:'openai_reply_ready',turnId:id,transcriptChars:transcript.length,replyChars:text.length,replyBytes:replyPcm.length,model}));
      return {caption:text,pcm:replyPcm,source:'openai'};
    })();
    promise.catch(()=>{});
    return {get status(){return job.status;}, promise, cancel:()=>controller.abort()};
  }
  return {submit};
}
module.exports={createOpenAIProvider};
