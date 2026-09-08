package com.steve.puckd

import android.app.*
import android.content.Intent
import android.os.*
import android.media.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/** All connection state is serialized on the main looper; old sockets cannot change new state. */
class PuckService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private var socket: WebSocket? = null
    private var stopped = false
    private var mediaToken = ""
    private var replyMode = "fixture"
    private var sequence = 0L
    private var pendingId: Long? = null
    private var sentAt = 0L
    private var attempt = 0
    private val session = UUID.randomUUID().toString()
    private val reconnect = Runnable { connect() }
    private val pulse = Runnable { heartbeat() }
    private val timeout = Runnable { disconnect("Heartbeat timed out") }
    private var wake: WakeWordClient? = null
    private val wakeExec = Executors.newSingleThreadExecutor()
    @Volatile private var wakeRunning = false

    override fun onCreate() {
        super.onCreate()
        Thread {
            try {
                val d = NativeWakeDetector(this, { s -> Log.i(TAG, "native_wake_score=$s") }, { Log.i(TAG, "native_wake_detected") })
                assets.open("wakeword/wake-sample.wav").use { it.skip(44); val raw=it.readBytes(); val pcm=ShortArray(raw.size/2) { i -> ((raw[i*2].toInt() and 255) or (raw[i*2+1].toInt() shl 8)).toShort() }; for (i in pcm.indices step 1280) d.accept(pcm.copyOfRange(i, minOf(i+1280, pcm.size))) }; d.close()
            } catch (e: Exception) { Log.e(TAG, "native wake probe failed", e) }
        }.start()
        Thread { WakeModelProbe.inspect(this) }.start()
        Thread { WakeModelProbe.inspect(this) }.start()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("puckd", "Puck connection", NotificationManager.IMPORTANCE_LOW))
        startForeground(1001, notification("Connecting"))
        startLocalWakeListener()
        connect()
    }

    private fun startLocalWakeListener() {
        if (wakeRunning) return
        wakeRunning = true
        getSharedPreferences("puck_status", MODE_PRIVATE).edit().putString("wakeState", "Wake word ready · local").apply()
        wakeExec.execute {
            try {
                val bytes=assets.open("microwakeword/hey_jarvis.tflite").use{it.readBytes()}; val model=ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()); model.put(bytes); model.rewind()
                val detector=MicroWakeWord(model,10,0.97f,5)
                var cooldownUntil = 0L
                val listener: (ShortArray)->Unit = { frame -> if (wakeRunning && SystemClock.elapsedRealtime() >= cooldownUntil && !getSharedPreferences("puck_status", MODE_PRIVATE).getBoolean("wakePending", false) && detector.processAudio(frame)) { Log.i(TAG,"microWakeWord DETECTED; handing off to turn"); getSharedPreferences("puck_status",MODE_PRIVATE).edit().putBoolean("wakePending",true).putString("wakeState","Hey Jarvis heard you").apply(); sendBroadcast(Intent("com.steve.puckd.WAKE")); detector.reset(); cooldownUntil = SystemClock.elapsedRealtime() + 2500 } }
                SharedAudioCapture.subscribe(listener)
                Log.i(TAG,"microWakeWord armed on shared microphone stream")
                while (wakeRunning && !stopped) Thread.sleep(250)
                SharedAudioCapture.unsubscribe(listener); detector.close()
            } catch(e:Exception){ Log.e(TAG,"local microWakeWord failed",e) }
        }
    }

    private fun connect() {
        if (stopped || socket != null) return
        update("Connecting", "Waiting for the relay")
        socket = client.newWebSocket(Request.Builder().url(BuildConfig.RELAY_URL).build(),
            object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) = dispatch(ws) {
                    update("Checking connection", "Waiting for acknowledgment")
                    heartbeat()
                }
                override fun onMessage(ws: WebSocket, text: String) = dispatch(ws) {
                    try {
                        val message = JSONObject(text)
                        when (message.optString("type")) {
                            "weather" -> {
                                val weather = message.optString("weatherText")
                                if (weather.isNotBlank()) getSharedPreferences("puck_status", MODE_PRIVATE).edit().putString("weatherText", weather).apply()
                            }
                            "hello" -> {
                                replyMode = when (message.optString("replyMode")) {
                                    "chatgpt_supervised" -> "chatgpt_supervised"
                                    "realtime" -> "realtime"
                                    else -> "fixture"
                                }
                                val token = message.optString("mediaToken")
                                val weather = message.optString("weatherText")
                                if (weather.isNotBlank()) getSharedPreferences("puck_status", MODE_PRIVATE).edit().putString("weatherText", weather).apply()
                                if (Regex("[0-9a-f]{64}").matches(token)) mediaToken = token
                                Log.i(TAG, "relay hello received; media enabled=${mediaToken.isNotEmpty()}")
                            }
                            "heartbeat_ack" -> {
                                if (message.optInt("protocol") == 1 &&
                                    message.optString("session") == session &&
                                    pendingId != null && message.optLong("id", -1) == pendingId) {
                                    val rtt = SystemClock.elapsedRealtime() - sentAt
                                    handler.removeCallbacks(timeout)
                                    pendingId = null
                                    attempt = 0
                                    update("Connected", "Round trip ${rtt} ms · heartbeat $sequence")
                                    Log.i(TAG, "heartbeat_ack id=$sequence rtt_ms=$rtt")
                                    handler.postDelayed(pulse, HEARTBEAT_MS)
                                }
                            }
                        }
                    } catch (e: Exception) { Log.w(TAG, "Ignored invalid relay message", e) }
                }
                override fun onClosing(ws: WebSocket, code: Int, reason: String) = dispatch(ws) {
                    ws.close(code, reason)
                    disconnect("Relay closed ($code)")
                }
                override fun onClosed(ws: WebSocket, code: Int, reason: String) = dispatch(ws) {
                    disconnect("Relay closed ($code)")
                }
                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) = dispatch(ws) {
                    Log.w(TAG, "Connection failed: ${t.message}")
                    disconnect("Relay unavailable")
                }
            })
    }

    private fun dispatch(ws: WebSocket, action: () -> Unit) {
        handler.post { if (!stopped && socket === ws) action() }
    }

    private fun heartbeat() {
        val ws = socket ?: return
        if (pendingId != null || stopped) return
        sequence++
        pendingId = sequence
        sentAt = SystemClock.elapsedRealtime()
        val payload = JSONObject().put("type", "heartbeat").put("protocol", 1)
            .put("id", sequence).put("session", session)
            .put("device", Build.DEVICE).put("model", Build.MODEL)
            .put("android", Build.VERSION.RELEASE)
            .put("deviceId", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
        if (!ws.send(payload.toString())) disconnect("Send failed")
        else handler.postDelayed(timeout, ACK_TIMEOUT_MS)
    }

    private fun disconnect(reason: String) {
        handler.removeCallbacks(pulse)
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(reconnect)
        val old = socket
        socket = null
        pendingId = null
        mediaToken = ""
        old?.cancel()
        if (stopped) return
        val delay = minOf(30_000L, 2_000L * (1L shl minOf(attempt++, 4)))
        update("Offline", "$reason · retry in ${delay / 1000}s")
        Log.i(TAG, "disconnected reason=$reason retry_ms=$delay")
        handler.postDelayed(reconnect, delay)
    }

    private fun notification(status: String): Notification = NotificationCompat.Builder(this, "puckd")
        .setContentTitle("Steve Puck").setContentText(status)
        .setSmallIcon(android.R.drawable.stat_sys_upload).setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build()

    private fun update(state: String, detail: String) {
        getSharedPreferences("puck_status", MODE_PRIVATE).edit()
            .putString("state", state).putString("detail", detail)
            .putString("mediaToken", if (state == "Connected") mediaToken else "")
            .putString("session", session)
            .putString("replyMode", replyMode)
            .putLong("updatedElapsed", SystemClock.elapsedRealtime()).apply()
        getSystemService(NotificationManager::class.java).notify(1001, notification("$state · $detail"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        socket?.cancel()
        socket = null
        wakeRunning = false
        wakeRunning=false
        SharedAudioCapture.stop()
        wakeExec.shutdownNow()
        wake?.stop(); wake = null
        update("Offline", "Service stopped")
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        super.onDestroy()
    }
    companion object {
        private const val TAG = "PuckService"
        private const val HEARTBEAT_MS = 15_000L
        private const val ACK_TIMEOUT_MS = 8_000L
    }
}
