package com.steve.puckd

import android.media.AudioFormat
import android.content.Context
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
class RealtimeTurnTransport(private val context: Context, private val sink: TurnTransportSink) : TurnTransport {
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private val pcm = ArrayList<Short>()
    private var listener: ((ShortArray) -> Unit)? = null
    private var socket: WebSocket? = null
    private var worker: Thread? = null
    @Volatile private var queue = LinkedBlockingQueue<ByteArray>()
    private val responseDone = AtomicBoolean(false)
    private var ready = CountDownLatch(1)
    private var turnNumber = 0
    private var turnId = ""
    private var transportGeneration = 0L
    private var conversationSnapshot = ConversationHistory.Snapshot("", 1, "", 0, 0)
    private var userTranscript = StringBuilder()
    private var assistantTranscript = StringBuilder()
    @Volatile override var busy = false
        private set
    @Volatile override var recording = false
        private set
    @Volatile private var cancelled = false

    override fun start() = start(null)

    override fun start(initialPcm: ShortArray?) {
        if (busy) return
        val key = BuildConfig.OPENAI_API_KEY
        if (key.isBlank()) { sink.onTransportError(IllegalStateException("OPENAI_API_KEY is missing")); return }
        busy = true; recording = true; cancelled = false; responseDone.set(false); ready = CountDownLatch(1)
        turnId = "rt-${++turnNumber}-${System.currentTimeMillis()}"; Log.i(TAG, "seq=1 turn=$turnId start thread=${Thread.currentThread().name}")
        val callbackGeneration = ++transportGeneration
        queue = LinkedBlockingQueue()
        conversationSnapshot = ConversationHistory.begin()
        userTranscript = StringBuilder()
        assistantTranscript = StringBuilder()
        synchronized(pcm) { pcm.clear() }
        initialPcm?.let { synchronized(pcm) { pcm.addAll(it.toList()) }; Log.i(TAG, "barge_preroll_pcm_bytes=${it.size * 2}") }
        listener = { frame -> synchronized(pcm) { if (recording && !cancelled) pcm.addAll(frame.toList()) } }
        Log.i(TAG, "media_turn_start_active turn=$turnId initial_preroll=${initialPcm?.size?.times(2) ?: 0}")
        SharedAudioCapture.beginTurn(); SharedAudioCapture.subscribe(listener!!)
        val request = Request.Builder().url("wss://api.openai.com/v1/realtime?model=gpt-realtime-2.1")
            .header("Authorization", "Bearer $key").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                if (!isCurrent(ws, callbackGeneration)) return
                Log.i(TAG, "seq=2 turn=$turnId connection_established thread=${Thread.currentThread().name}")
                val turnQueue = queue
                worker = Thread { playbackLoop(turnQueue, callbackGeneration) }.also { it.start() }
                val configuredVoice = RealtimeVoicePreferences.get(context); Log.i(TAG, "configured_voice=$configuredVoice turn=$turnId")
                val stableInstructions = "Speak in a clear, natural conversational male voice. Keep the pitch in a normal mid-range, not unusually deep or boomy. Use crisp articulation and a slightly brisk conversational pace. Avoid a slow announcer cadence or exaggerated bass-heavy delivery. Sound relaxed, intelligent, and informal."
                val contextInstructions = if (conversationSnapshot.text.isBlank()) stableInstructions else "$stableInstructions\n\nRecent conversation context. Use it only to resolve references and maintain continuity:\n${conversationSnapshot.text}"
                val sessionUpdate = JSONObject().put("type", "session.update").put("session", JSONObject()
                    .put("type", "realtime").put("output_modalities", org.json.JSONArray().put("audio"))
                    .put("instructions", contextInstructions)
                    .put("audio", JSONObject().put("input", JSONObject()
                        .put("format", JSONObject().put("type", "audio/pcm").put("rate", 24000))
                        .put("transcription", JSONObject().put("model", "gpt-4o-mini-transcribe"))
                        .put("turn_detection", JSONObject.NULL))
                        .put("output", JSONObject().put("format", JSONObject().put("type", "audio/pcm").put("rate", 24000)).put("voice", configuredVoice))))
                checkSend(ws, "session.update", sessionUpdate.toString())
            }
            override fun onMessage(ws: WebSocket, text: String) {
                if (!isCurrent(ws, callbackGeneration)) { Log.i(TAG, "stale_event_ignored generation=$callbackGeneration"); return }
                try {
                    val event = JSONObject(text); val type = event.optString("type")
                    Log.i(TAG, "seq=event turn=$turnId server_event=$type thread=${Thread.currentThread().name}")
                    if (type == "session.created" || type == "session.updated" || type == "error") {
                        Log.i(TAG, "full_payload turn=$turnId $text")
                    }
                    if (type == "session.created" || type == "session.updated") {
                        val session = event.optJSONObject("session")
                        Log.i(TAG, "server_session_config turn=$turnId type=$type voice=${session?.opt("voice")} audio=${session?.optJSONObject("audio")} output_modalities=${session?.opt("output_modalities")}")
                        Log.i(TAG, "session.$type.audio.output.voice=${session?.optJSONObject("audio")?.optJSONObject("output")?.opt("voice")} turn=$turnId")
                    }
                    if (type == "conversation.item.input_audio_transcription.completed") {
                        val transcript = event.optString("transcript")
                        if (transcript.isNotBlank()) userTranscript.append(transcript).append(' ')
                        Log.i(TAG, "user_transcript_received chars=${transcript.length} turn=$turnId")
                    }
                    if (type == "response.output_audio_transcript.delta" || type == "response.audio_transcript.delta") {
                        val delta = event.optString("delta")
                        if (delta.isNotBlank()) assistantTranscript.append(delta)
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
                        ConversationHistory.complete(userTranscript.toString(), assistantTranscript.toString())
                    } else if (type == "error") {
                        val e = event.optJSONObject("error")
                        fail(Exception("${e?.optString("code")}: ${e?.optString("message")}"))
                    }
                } catch (e: Exception) { fail(e, ws, callbackGeneration) }
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) { fail(t, ws, callbackGeneration) }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) { if (isCurrent(ws, callbackGeneration)) Log.i(TAG, "closed code=$code reason=$reason") else Log.i(TAG, "stale_close_ignored generation=$callbackGeneration") }
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

    override fun cancel() {
        cancelled = true; transportGeneration++
        recording = false; listener?.let { SharedAudioCapture.unsubscribe(it) }
        runCatching { SharedAudioCapture.endTurn() }
        val oldQueue = queue
        oldQueue.clear(); oldQueue.offer(ByteArray(0))
        socket?.cancel(); busy = false
        Log.i(TAG, "transport_cancelled generation=$transportGeneration")
    }
    override fun connectionChanged(token: String) {}
    override fun close() { cancel(); client.dispatcher.executorService.shutdown() }

    private fun playbackLoop(turnQueue: LinkedBlockingQueue<ByteArray>, generation: Long) {
        while (!cancelled && generation == transportGeneration) {
            val chunk = turnQueue.take()
            if (chunk.isEmpty() && (responseDone.get() || generation != transportGeneration || cancelled)) break
            if (chunk.isNotEmpty()) { Log.i(TAG, "playback_worker_started_bytes=${chunk.size}"); sink.onResponseAudio(chunk, TurnAudioFormat(24000, 1, AudioFormat.ENCODING_PCM_16BIT)); Log.i(TAG, "playback_worker_finished_bytes=${chunk.size}") }
        }
        if (generation != transportGeneration || cancelled) { Log.i(TAG, "stale_playback_worker_stopped generation=$generation"); return }
        Log.i(TAG, "playback_queue_drained")
        if (responseDone.get() && !cancelled) { sink.onResponseCompleted(); busy = false; Log.i(TAG, "final_turn_complete") }
    }
    private fun isCurrent(ws: WebSocket, generation: Long): Boolean = generation == transportGeneration && socket === ws && !cancelled
    private fun fail(error: Throwable, ws: WebSocket? = socket, generation: Long = transportGeneration) {
        if (ws != null && !isCurrent(ws, generation)) { Log.i(TAG, "stale_failure_ignored generation=$generation"); return }
        if (!cancelled) { Log.e(TAG, "transport_error=${error.message}"); sink.onTransportError(error); cancelled = true; busy = false }
    }
    private fun checkSend(ws: WebSocket, label: String, payload: String) { val ok = ws.send(payload); Log.i(TAG, "seq=send turn=$turnId op=$label accepted=$ok thread=${Thread.currentThread().name}"); if (!ok) throw IllegalStateException("WebSocket rejected $label") }
    companion object { private const val TAG = "RealtimeTransport" }
}
