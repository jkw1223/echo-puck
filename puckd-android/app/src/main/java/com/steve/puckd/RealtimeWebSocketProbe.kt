package com.steve.puckd

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object RealtimeWebSocketProbe {
    private const val TAG="RealtimeProbe"
    fun run() {
        require(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "OPENAI_API_KEY missing" }
        val client=OkHttpClient.Builder().readTimeout(0,TimeUnit.MILLISECONDS).build()
        val req=Request.Builder().url("wss://api.openai.com/v1/realtime?model=gpt-realtime-2.1").header("Authorization","Bearer ${BuildConfig.OPENAI_API_KEY}").build()
        client.newWebSocket(req, object:WebSocketListener(){
            override fun onOpen(ws:WebSocket,response:Response){
                Log.i(TAG,"connection_opened")
                ws.send("{\"type\":\"session.update\",\"session\":{\"type\":\"realtime\",\"output_modalities\":[\"text\"]}}")
                ws.send("{\"type\":\"conversation.item.create\",\"item\":{\"type\":\"message\",\"role\":\"user\",\"content\":[{\"type\":\"input_text\",\"text\":\"Reply with exactly: PUCK DIRECT REALTIME WORKS\"}]}}")
                ws.send("{\"type\":\"response.create\",\"response\":{\"output_modalities\":[\"text\"]}}")
            }
            override fun onMessage(ws:WebSocket,text:String){ try { val e=JSONObject(text); val type=e.optString("type"); Log.i(TAG,"server_event=$type"); if(type=="response.output_text.delta") Log.i(TAG,"response_text_delta="+e.optString("delta")); if(type=="response.done"){Log.i(TAG,"response_completed"); ws.close(1000,"probe complete"); client.dispatcher.executorService.shutdown()} ; if(type=="error"){val er=e.optJSONObject("error"); Log.e(TAG,"api_error code="+er?.optString("code")+" message="+er?.optString("message"))} } catch(ex:Exception){Log.e(TAG,"event_parse_error "+ex.message)} }
            override fun onFailure(ws:WebSocket,t:Throwable,response:Response?){Log.e(TAG,"connection_failed "+t.message)}
            override fun onClosed(ws:WebSocket,code:Int,reason:String){Log.i(TAG,"closed code=$code reason=$reason")}
        })
    }
}
