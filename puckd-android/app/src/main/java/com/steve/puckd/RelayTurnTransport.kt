package com.steve.puckd

import android.content.Context
import android.media.*
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** One foreground turn at a time; no retries or queued speech across a connection change. */
class RelayTurnTransport(private val context: Context, private val report: (String, String) -> Unit, private val sink: TurnTransportSink) : TurnTransport {
    private val worker = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).build()
    private val baseUrl = BuildConfig.RELAY_URL.replaceFirst("ws://", "http://")
        .replaceFirst("wss://", "https://").removeSuffix("/puck")
    @Volatile override var busy = false
        private set
    @Volatile override var recording = false
        private set
    @Volatile private var finished = false
    @Volatile private var cancelled = false
    @Volatile private var closed = false
    @Volatile private var call: Call? = null
    @Volatile private var token = ""
    @Volatile private var session = ""
    @Volatile private var turnId = ""

    override fun finish() { if (recording) { finished = true; SharedAudioCapture.requestFinish() } }
    override fun cancel() {
        if (!busy || cancelled) return
        cancelled = true
        call?.cancel()
        if (turnId.isNotEmpty() && token.isNotEmpty()) {
            client.newCall(request("DELETE").build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) { response.close() }
            })
        }
    }
    override fun connectionChanged(currentToken: String) { if (busy && currentToken != token) cancel() }
    override fun close() { closed = true; cancel(); worker.shutdown() }

    override fun start() {
        if (busy || closed) return
        val prefs = context.getSharedPreferences("puck_status", Context.MODE_PRIVATE)
        token = prefs.getString("mediaToken", "") ?: ""
        session = prefs.getString("session", "") ?: ""
        if (token.isEmpty()) { report("Offline", "Wait for the Mac connection before talking."); return }
        turnId = UUID.randomUUID().toString()
        finished = false; cancelled = false; busy = true; recording = true
        report("Listening", "Speak, then tap Finish · 30 seconds maximum")
        worker.execute {
            var failure: String? = null
            try {
                val pcm = capture()
                recording = false
                checkActive()
                check(pcm.size >= 3200) { "Recording too short. Try again." }
                report("Sending", "Sending your recorded turn to the Mac")
                val sha = MessageDigest.getInstance("SHA-256").digest(pcm).joinToString("") { "%02x".format(it) }
                val accepted = exchange(request("POST", pcm).build(), 8192)
                check(accepted.optString("type") == "turn_received" && accepted.optString("turnId") == turnId &&
                    accepted.optInt("receivedBytes") == pcm.size && accepted.optString("sha256") == sha) { "Upload acknowledgment mismatch" }
                Log.i(TAG, "turn_received id=$turnId bytes=${pcm.size} sha256=$sha")
                checkActive()
                report("Waiting for reply", "Mac received ${pcm.size / 32} ms of audio")
                val deadline = SystemClock.elapsedRealtime() + 230_000
                var response: JSONObject
                var lastStatus = ""
                while (true) {
                    checkActive()
                    check(SystemClock.elapsedRealtime() < deadline) { "Conversation timed out. Start a new turn." }
                    response = exchange(request("GET", reply = true).build(), 1_400_000)
                    check(response.optString("turnId") == turnId) { "Reply turn ID mismatch" }
                    if (response.optString("type") != "turn_pending") break
                    val stage = response.optString("status")
                    if (stage != lastStatus) {
                        when(stage) {
                            "transcribing" -> report("Transcribing", "The Mac is converting your speech to text")
                            "awaiting_browser", "browser_claimed" -> report("Waiting for ChatGPT", "Supervised bridge · browser operator is handling this turn")
                            "synthesizing" -> report("Preparing voice", "The Mac is preparing the spoken answer")
                            else -> report("Waiting for reply", "Conversation in progress")
                        }
                        lastStatus = stage
                    }
                    for (i in 0 until 10) { checkActive(); Thread.sleep(100) }
                }
                checkActive()
                check(response.optString("type") == "turn_reply" && response.optString("turnId") == turnId &&
                    response.optString("format") == "pcm_s16le_16000_mono" && response.optString("source") in setOf("local_fixture", "chatgpt_web", "openai_realtime")) { "Unexpected reply" }
                val caption = response.getString("caption")
                check(caption.length <= 1000) { "Reply caption too long" }
                val reply = Base64.decode(response.getString("pcmBase64"), Base64.NO_WRAP)
                check(reply.isNotEmpty() && reply.size <= 960000 && reply.size % 2 == 0) { "Invalid reply audio" }
                checkActive()
                report(if (response.optString("source") == "chatgpt_web" || response.optString("source") == "openai_realtime") "Steve · OpenAI reply" else "Speaking · bridge test", caption)
                sink.onResponseAudio(reply, TurnAudioFormat(16000, 1, AudioFormat.ENCODING_PCM_16BIT))
                sink.onResponseCompleted()
                checkActive()
                Log.i(TAG, "turn_complete id=$turnId reply_bytes=${reply.size}")
                report("Ready · tap Talk", caption)
            } catch (e: Exception) {
                if (!cancelled) {
                    Log.w(TAG, "turn_failed id=$turnId reason=${e.message}")
                    failure = e.message ?: "Try again"
                    // Best-effort withdrawal; never retry the audio upload.
                    cancel()
                }
            } finally {
                recording = false; busy = false; call = null
                if (failure != null) report("Turn failed", failure!!)
                else if (cancelled) report("Stopped", "Microphone off · no turn will replay")
            }
        }
    }
    private fun checkActive() {
        val current = context.getSharedPreferences("puck_status", Context.MODE_PRIVATE).getString("mediaToken", "")
        if (cancelled || closed || current != token) { cancelled = true; throw IOException("Turn canceled or connection changed") }
    }
    private fun request(method: String, pcm: ByteArray? = null, reply: Boolean = false): Request.Builder {
        val builder = Request.Builder().url("$baseUrl/turns/$turnId" + if (reply) "/reply" else "")
            .header("Authorization", "Bearer $token").header("X-Puck-Session", session)
        return when(method) {
            "POST" -> builder.header("X-Puck-Format", "pcm_s16le_16000_mono")
                .post(pcm!!.toRequestBody("application/octet-stream".toMediaType()))
            "DELETE" -> builder.delete()
            else -> builder.get()
        }
    }
    private fun exchange(request: Request, maxBytes: Long): JSONObject {
        checkActive()
        val activeCall = client.newCall(request); call = activeCall
        // cancel() may race with publishing this call; recheck after publication.
        if (cancelled) activeCall.cancel()
        activeCall.execute().use { response ->
            checkActive()
            check(response.isSuccessful) { "Mac returned HTTP ${response.code}; see relay diagnostics" }
            val source = response.body?.source() ?: error("Empty relay response")
            source.request(maxBytes + 1)
            check(source.buffer.size <= maxBytes) { "Reply exceeded size limit" }
            val data = source.readByteArray()
            return JSONObject(String(data, Charsets.UTF_8))
        }
    }
    private fun capture(): ByteArray {
        SharedAudioCapture.beginTurn()
        try {
            val deadline = SystemClock.elapsedRealtime() + 30000
            while (!SharedAudioCapture.isFinishRequested() && SystemClock.elapsedRealtime() < deadline) { checkActive(); Thread.sleep(20) }
            val pcm = SharedAudioCapture.endTurn()
            check(pcm.size >= 3200) { "Recording too short. Try again." }
            Log.i(TAG, "shared_capture_complete id=$turnId bytes=${pcm.size}")
            return pcm
        } finally { if (!SharedAudioCapture.isFinishRequested()) SharedAudioCapture.endTurn() }
    }
    companion object { private const val TAG = "PuckMedia" }
}
