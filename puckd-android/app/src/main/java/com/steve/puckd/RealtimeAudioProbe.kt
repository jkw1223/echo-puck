package com.steve.puckd

import android.media.AudioFormat
import android.util.Base64
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

object RealtimeAudioProbe {
 private const val TAG="RealtimeAudioProbe"
 fun run(){ require(BuildConfig.OPENAI_API_KEY.isNotBlank()){"OPENAI_API_KEY missing"}; val client=OkHttpClient.Builder().readTimeout(0,TimeUnit.MILLISECONDS).build(); val playback=TurnAudioPlayback(); val queue=LinkedBlockingQueue<ByteArray>(); val responseDone=AtomicBoolean(false); val pcm=ArrayList<Short>(); var wsRef:WebSocket?=null
  val req=Request.Builder().url("wss://api.openai.com/v1/realtime?model=gpt-realtime-2.1").header("Authorization","Bearer ${BuildConfig.OPENAI_API_KEY}").build()
  client.newWebSocket(req,object:WebSocketListener(){
   override fun onOpen(ws:WebSocket,r:Response){wsRef=ws; Thread { while(true){ val chunk=queue.take(); if(chunk.isEmpty() && responseDone.get()) break; if(chunk.isNotEmpty()){ Log.i(TAG,"playback_worker_started_bytes=${chunk.size}"); playback.play(chunk){}; Log.i(TAG,"playback_worker_finished_bytes=${chunk.size}") } }; Log.i(TAG,"playback_queue_drained"); if(responseDone.get()) { Log.i(TAG,"final_turn_complete"); ws.close(1000,"audio probe complete") } }.start(); Log.i(TAG,"connection_established_direct_show"); ws.send("{\"type\":\"session.update\",\"session\":{\"type\":\"realtime\",\"output_modalities\":[\"audio\"],\"audio\":{\"input\":{\"format\":{\"type\":\"audio/pcm\",\"rate\":24000}},\"output\":{\"format\":{\"type\":\"audio/pcm\",\"rate\":24000}}}}}"); val l:(ShortArray)->Unit={f->synchronized(pcm){f.forEach{pcm.add(it)}}}; SharedAudioCapture.subscribe(l); Thread{Thread.sleep(5000); SharedAudioCapture.unsubscribe(l); val b=ByteArray(pcm.size*2); pcm.forEachIndexed{i,v->b[i*2]=v.toByte();b[i*2+1]=(v.toInt() shr 8).toByte()}; Log.i(TAG,"input_pcm_bytes=${b.size}"); ws.send(JSONObject().put("type","input_audio_buffer.append").put("audio",Base64.encodeToString(b,Base64.NO_WRAP)).toString()); Log.i(TAG,"input_audio_append_bytes=${b.size}"); ws.send("{\"type\":\"input_audio_buffer.commit\"}"); Log.i(TAG,"input_audio_committed"); ws.send("{\"type\":\"response.create\",\"response\":{\"output_modalities\":[\"audio\"]}}")
Log.i(TAG,"response_requested")}.start() }
   override fun onMessage(ws:WebSocket,t:String){try{val e=JSONObject(t); val type=e.optString("type"); Log.i(TAG,"server_event=$type"); if(type=="response.audio.delta"||type=="response.output_audio.delta"){val d=Base64.decode(e.getString("delta"),Base64.NO_WRAP); Log.i(TAG,"audio_delta_bytes=${d.size}"); queue.put(d); Log.i(TAG,"audio_chunk_queued_bytes=${d.size}") }; if(type=="response.done"){Log.i(TAG,"response_done_received"); responseDone.set(true); queue.put(ByteArray(0));}; if(type=="error"){val er=e.optJSONObject("error");Log.e(TAG,"api_error code=${er?.optString("code")} message=${er?.optString("message")}")}}catch(x:Exception){Log.e(TAG,"event_error ${x.message}")}}
   override fun onFailure(ws:WebSocket,t:Throwable,r:Response?){Log.e(TAG,"connection_failed ${t.message}")}; override fun onClosed(ws:WebSocket,c:Int,reason:String){Log.i(TAG,"closed code=$c reason=$reason")}
  })
 }
}
