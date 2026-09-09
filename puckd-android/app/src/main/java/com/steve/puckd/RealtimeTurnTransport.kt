package com.steve.puckd

import android.media.AudioFormat
import android.util.Base64
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/** Direct OpenAI Realtime transport. The relay transport remains the default rollback path. */
class RealtimeTurnTransport(private val sink: TurnTransportSink) : TurnTransport {
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private val pcm = ArrayList<Short>()
    private var listener: ((ShortArray) -> Unit)? = null
    private var socket: WebSocket? = null
    private var worker: Thread? = null
    private val queue = LinkedBlockingQueue<ByteArray>()
    private val responseDone = AtomicBoolean(false)
    private var ready = CountDownLatch(1)
    private var turnNumber = 0
    private var turnId = ""
    @Volatile override var busy = false
        private set
    @Volatile override var recording = false
        private set
    @Volatile private var cancelled = false

    override fun start() {
        if (busy) return
        val key = BuildConfig.OPENAI_API_KEY
        if (key.isBlank()) { sink.onTransportError(IllegalStateException("OPENAI_API_KEY is missing")); return }
        busy = true; recording = true; cancelled = false; responseDone.set(false); ready = CountDownLatch(1)
        turnId = "rt-${++turnNumber}-${System.currentTimeMillis()}"; Log.i(TAG, "seq=1 turn=$turnId start thread=${Thread.currentThread().name}")
        synchronized(pcm) { pcm.clear() }
        listener = { frame -> synchronized(pcm) { if (recording && !cancelled) pcm.addAll(frame.toList()) } }
        SharedAudioCapture.beginTurn(); SharedAudioCapture.subscribe(listener!!)
        val request = Request.Builder().url("wss://api.openai.com/v1/realtime?model=gpt-realtime-2.1")
            .header("Authorization", "Bearer $key").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(TAG, "seq=2 turn=$turnId connection_established thread=${Thread.currentThread().name}")
                worker = Thread { playbackLoop() }.also { it.start() }
                checkSend(ws, "session.update", "{\"type\":\"session.update\",\"session\":{\"type\":\"realtime\",\"output_modalities\":[\"audio\"],\"audio\":{\"input\":{\"format\":{\"type\":\"audio/pcm\",\"rate\":24000},\"turn_detection\":null},\"output\":{\"format\":{\"type\":\"audio/pcm\",\"rate\":24000}}}}}")
            }
            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val event = JSONObject(text); val type = event.optString("type")
                    Log.i(TAG, "seq=event turn=$turnId server_event=$type thread=${Thread.currentThread().name}")
                    if (type == "session.created" || type == "session.updated" || type == "error") {
                        Log.i(TAG, "full_payload turn=$turnId $text")
                    }
                    if (type == "input_audio_buffer.speech_started" || type == "input_audio_buffer.speech_stopped") {
                        Log.i(TAG, "SPEECH_EVENT turn=$turnId type=$type")
                    }
                    if (type == "session.updated" || type == "session.created") ready.countDown()
                    if (type == "response.audio.delta" || type == "response.output_audio.delta") {
                        val data = Base64.decode(event.getString("delta"), Base64.NO_WRAP)
                        queue.put(data); Log.i(TAG, "audio_chunk_queued_bytes=${data.size}")
                    } else if (type == "response.done") {
                        responseDone.set(true); queue.put(ByteArray(0)); Log.i(TAG, "response_done_received")
                    } else if (type == "error") {
                        val e = event.optJSONObject("error")
                        fail(Exception("${e?.optString("code")}: ${e?.optString("message")}"))
                    }
                } catch (e: Exception) { fail(e) }
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) { fail(t) }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) { Log.i(TAG, "closed code=$code reason=$reason") }
        })
    }

    override fun finish() {
        if (!recording || cancelled) return
        Log.i(TAG, "seq=finish turn=$turnId thread=${Thread.currentThread().name}"); recording = false
        listener?.let { SharedAudioCapture.unsubscribe(it) }
        val raw = SharedAudioCapture.endTurn()
        val ws = socket ?: run { fail(IllegalStateException("socket unavailable")); return }
        if (!ready.await(10, TimeUnit.SECONDS)) { fail(IllegalStateException("Realtime session not ready")); return }
        Log.i(TAG, "input_pcm_bytes=${raw.size}")
        if (raw.isEmpty()) { fail(IllegalStateException("no microphone PCM captured")); return }
        checkSend(ws, "input_audio_buffer.append bytes=${raw.size}", JSONObject().put("type", "input_audio_buffer.append").put("audio", Base64.encodeToString(raw, Base64.NO_WRAP)).toString())
        checkSend(ws, "input_audio_buffer.commit", "{\"type\":\"input_audio_buffer.commit\"}")
        checkSend(ws, "response.create", "{\"type\":\"response.create\",\"response\":{\"output_modalities\":[\"audio\"]}}")
        Log.i(TAG, "input_audio_committed response_requested")
    }

    override fun cancel() { cancelled = true; recording = false; listener?.let { SharedAudioCapture.unsubscribe(it) }; runCatching { SharedAudioCapture.endTurn() }; queue.clear(); socket?.cancel(); busy = false }
    override fun connectionChanged(token: String) {}
    override fun close() { cancel(); client.dispatcher.executorService.shutdown() }

    private fun playbackLoop() {
        while (!cancelled) {
            val chunk = queue.take()
            if (chunk.isEmpty() && responseDone.get()) break
            if (chunk.isNotEmpty()) { Log.i(TAG, "playback_worker_started_bytes=${chunk.size}"); sink.onResponseAudio(chunk, TurnAudioFormat(24000, 1, AudioFormat.ENCODING_PCM_16BIT)); Log.i(TAG, "playback_worker_finished_bytes=${chunk.size}") }
        }
        Log.i(TAG, "playback_queue_drained")
        if (responseDone.get() && !cancelled) { sink.onResponseCompleted(); busy = false; Log.i(TAG, "final_turn_complete") }
    }
    private fun fail(error: Throwable) { if (!cancelled) { Log.e(TAG, "transport_error=${error.message}"); sink.onTransportError(error); cancelled = true; busy = false } }
    private fun checkSend(ws: WebSocket, label: String, payload: String) { val ok = ws.send(payload); Log.i(TAG, "seq=send turn=$turnId op=$label accepted=$ok thread=${Thread.currentThread().name}"); if (!ok) throw IllegalStateException("WebSocket rejected $label") }
    companion object { private const val TAG = "RealtimeTransport" }
}
