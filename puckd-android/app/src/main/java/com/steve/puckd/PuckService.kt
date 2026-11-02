package com.steve.puckd

import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

/** Foreground owner for local wake detection and shared microphone capture. */
class PuckService : Service() {
    private var stopped = false
    private val wakeExec = Executors.newSingleThreadExecutor()
    @Volatile private var wakeRunning = false
    private val statusPrefs by lazy { getSharedPreferences("puck_status", MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("puckd", "Puck audio", NotificationManager.IMPORTANCE_LOW))
        startForeground(1001, notification("Direct Realtime · ready"))
        statusPrefs.edit()
            .putString("state", "Direct Realtime")
            .putString("detail", "Ready for a direct voice turn")
            .putString("wakeState", "Wake word ready · local")
            .putBoolean("wakeArmed", true)
            .remove("transport")
            .remove("mediaToken")
            .remove("session")
            .remove("replyMode")
            .remove("weatherText")
            .apply()
        startLocalWakeListener()
    }

    private fun startLocalWakeListener() {
        if (wakeRunning) return
        wakeRunning = true
        getSharedPreferences("puck_status", MODE_PRIVATE).edit().putString("wakeState", "Wake word ready · local").putBoolean("wakeArmed", true).apply()
        wakeExec.execute {
            try {
                val bytes=assets.open("microwakeword/hey_jarvis.tflite").use{it.readBytes()}; val model=ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()); model.put(bytes); model.rewind()
                var detector=MicroWakeWord(model,10,0.97f,5)
                var detectorCutoff=0.97f
                var detectorWindow=5
                var probationUntil=0L
                Log.i(TAG,"wake_frame_ms=10 wake_cutoff=0.97 wake_window_frames=5")
                var cooldownUntil = 0L
                var wasArmed = true
                var playbackWakeFrames = 0L
                var playbackWakeEvaluations = 0L
                var playbackWakeLastLog = SystemClock.elapsedRealtime()
                val listener: (ShortArray)->Unit = { frame ->
                    val prefs=getSharedPreferences("puck_status", MODE_PRIVATE); val now=SystemClock.elapsedRealtime(); val armed=prefs.getBoolean("wakeArmed", true)
                    val playbackMode = prefs.getString("wakeMode", "idle") == "playback"
                    if (playbackMode) {
                        var sum = 0.0
                        frame.forEach { sample -> sum += sample.toDouble() * sample.toDouble() }
                        val rms = kotlin.math.sqrt(sum / frame.size).toInt()
                        playbackWakeFrames++
                        if (now - playbackWakeLastLog >= 1000L) {
                            Log.i(TAG, "playback_mic_rms=$rms playback_wake_mode=true")
                            Log.i(TAG, "playback_wake_frames_received=$playbackWakeFrames playback_wake_evaluations=$playbackWakeEvaluations")
                            playbackWakeFrames = 0L; playbackWakeEvaluations = 0L; playbackWakeLastLog = now
                        }
                    }
                    if (playbackMode && detectorCutoff != 0.99f) {
                        detector.close()
                        detector = MicroWakeWord(model, 10, 0.99f, 8)
                        detectorCutoff = 0.99f; detectorWindow = 8; probationUntil = 0L
                        Log.i(TAG, "wake_mode=playback wake_frame_ms=10 wake_cutoff=0.99 wake_required_hits=8")
                    } else if (!playbackMode && armed && !wasArmed) {
                        detector.close(); detector=MicroWakeWord(model,10,0.99f,8); detectorCutoff=0.99f; detectorWindow=8; probationUntil=now+3000L
                        Log.i(TAG,"wake_detector_reset"); Log.i(TAG,"wake_probation_begin cutoff=0.99 required_hits=8 duration_ms=3000")
                    }
                    if (!playbackMode && armed && probationUntil>0L && now>=probationUntil && detectorCutoff!=0.97f) {
                        detector.close(); detector=MicroWakeWord(model,10,0.97f,5); detectorCutoff=0.97f; detectorWindow=5; probationUntil=0L
                        Log.i(TAG,"wake_mode=idle wake_frame_ms=10 wake_cutoff=0.97 wake_required_hits=5")
                    }
                    wasArmed=armed
                    if (playbackMode && armed) playbackWakeEvaluations++
                    if (wakeRunning && armed && now >= cooldownUntil && !prefs.getBoolean("wakePending", false) && detector.processAudio(frame)) {
                        Log.i(TAG,"wake_detected=true wake_score=unavailable wake_cutoff=$detectorCutoff wake_window_hits=$detectorWindow")
                        if (probationUntil>now) {
                            Log.i(TAG,"wake_candidate_rejected reason=post_rearm_probation")
                            detector.reset()
                        } else {
                            Log.i(TAG,"microWakeWord DETECTED; handing off to turn")
                            prefs.edit().putBoolean("wakePending",true).putBoolean("wakeArmed",false).putString("wakeState","Hey Jarvis heard you").apply(); sendBroadcast(Intent("com.steve.puckd.WAKE")); detector.reset(); cooldownUntil = now + 2500L
                        }
                    }
                }
                SharedAudioCapture.subscribe(listener)
                Log.i(TAG,"microWakeWord armed on shared microphone stream")
                while (wakeRunning && !stopped) Thread.sleep(250)
                SharedAudioCapture.unsubscribe(listener); detector.close()
            } catch(e:Exception){ Log.e(TAG,"local microWakeWord failed",e) }
        }
    }

    private fun notification(status: String): Notification = NotificationCompat.Builder(this, "puckd")
        .setContentTitle("Steve Puck")
        .setContentText(status)
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopped = true
        wakeRunning = false
        statusPrefs.edit()
            .putString("state", "Stopped")
            .putString("detail", "Local audio service stopped")
            .putBoolean("wakeArmed", false)
            .apply()
        SharedAudioCapture.stop()
        wakeExec.shutdownNow()
        super.onDestroy()
    }

    companion object { private const val TAG = "PuckService" }
}
