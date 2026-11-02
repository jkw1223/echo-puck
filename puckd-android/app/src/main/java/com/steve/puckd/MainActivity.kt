package com.steve.puckd

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.view.WindowManager
import android.view.View
import android.widget.*
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.BroadcastReceiver
import android.content.IntentFilter

class MainActivity : AppCompatActivity() {
    private enum class HandsFreeState { WAKE_ARMED, LISTENING, PROCESSING, SPEAKING, INTERRUPTING, REARMING }
    private var handsFreeState = HandsFreeState.WAKE_ARMED
    private var eosFired = false
    private var speechStarted = false
    private var noiseCalibration = ArrayList<Int>()
    private var noiseFloor = 0
    private var speechThreshold = 400
    private var lastRmsLogAt = 0L
    private var listeningFrames = 0
    private val preroll = ArrayDeque<Short>()
    private val prerollMaxSamples = 12_000
    private var lastKnownNoiseFloor = 0
    private var bargeStartedAt = 0L
    private var firstListeningPcmLogged = false
    private var silenceMs = 0L
    private var listenStartedAt = 0L
    private val silenceEndMs = 850L
    private val maxUtteranceMs = 25_000L
    private val handsFreeListener: (ShortArray) -> Unit = { frame -> analyzeHandsFreeAudio(frame) }
    private val handsFreeLifecycle = object : TurnTransportSink {
        override fun onResponseAudio(pcm: ByteArray, format: TurnAudioFormat) {}
        override fun onResponseAudioStarted() {
            transitionHandsFree(HandsFreeState.SPEAKING, "response_audio_started")
            getSharedPreferences("puck_status", 0).edit().putBoolean("wakeArmed", true).putString("wakeMode", "playback").apply()
            android.util.Log.i("HandsFree", "playback_wake_enabled cutoff=0.99 required_hits=8")
        }
        override fun onResponseCompleted() {
            getSharedPreferences("puck_status", 0).edit().putBoolean("wakeArmed", false).putString("wakeMode", "idle").apply()
            transitionHandsFree(HandsFreeState.REARMING, "playback_complete")
            beginWakeRearm()
        }
        override fun onTransportError(error: Throwable) {
            android.util.Log.e("HandsFree", "turn_failure=" + error.message)
            getSharedPreferences("puck_status", 0).edit().putBoolean("wakeArmed", true).apply()
            transitionHandsFree(HandsFreeState.REARMING, "turn_failure")
            beginWakeRearm()
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var state: TextView
    private lateinit var detail: TextView
    private lateinit var turnState: TextView
    private lateinit var caption: TextView
    private lateinit var modeHint: TextView
    private lateinit var talk: Button
    private lateinit var speaker: Button
    private lateinit var settingsPanel: LinearLayout
    private lateinit var weatherView: TextView
    private lateinit var darkButton: Button
    private lateinit var wakeStatus: TextView
    private lateinit var wakeBar: View
    private var darkMode = false
    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: android.content.Context?, i: Intent?) {
            if (handsFreeState == HandsFreeState.SPEAKING) {
                if (turn.busy) {
                    bargeStartedAt = SystemClock.elapsedRealtime()
                    android.util.Log.i("HandsFree", "barge_t+0ms wake_accepted")
                    android.util.Log.i("HandsFree", "barge_in_detected")
                    transitionHandsFree(HandsFreeState.INTERRUPTING, "wake_detected")
                    getSharedPreferences("puck_status", 0).edit().putBoolean("wakeArmed", false).putString("wakeMode", "idle").apply()
                    android.util.Log.i("HandsFree", "barge_t+${SystemClock.elapsedRealtime()-bargeStartedAt}ms playback_cancel_requested")
                    turn.interruptCurrentResponse()
                    android.util.Log.i("HandsFree", "barge_t+${SystemClock.elapsedRealtime()-bargeStartedAt}ms playback_flushed")
                    android.util.Log.i("HandsFree", "response_interrupted")
                    resetListeningState(true)
                    transitionHandsFree(HandsFreeState.LISTENING, "barge_in_ready")
                    val bridge = synchronized(preroll) { preroll.toShortArray() }
                    android.util.Log.i("HandsFree", "barge_t+${SystemClock.elapsedRealtime()-bargeStartedAt}ms media_turn_start preroll_bytes=${bridge.size * 2}")
                    turn.start(bridge)
                }
                return
            }
            if (handsFreeState != HandsFreeState.WAKE_ARMED || turn.busy || audio.busy) { android.util.Log.i("HandsFree", "wake_ignored"); return }
            getSharedPreferences("puck_status", 0).edit().putBoolean("wakeArmed", false).putBoolean("wakePending", false).apply()
            resetListeningState(false)
            transitionHandsFree(HandsFreeState.LISTENING, "wake_detected")
            turn.start()
        }
    }
    private lateinit var audio: LocalAudio
    private lateinit var turn: MediaTurn
    private var visible = false
    private val refresh = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("puck_status", MODE_PRIVATE)
            if (prefs.getBoolean("wakePending", false) && handsFreeState == HandsFreeState.WAKE_ARMED && !audio.busy) {
                android.util.Log.i("WakeHandoff", "starting turn from durable wake; turnBusy=${turn.busy} audioBusy=${audio.busy}")
                prefs.edit().putBoolean("wakePending", false).putBoolean("wakeArmed", false).apply()
                wakeStatus.text = "●  Hey Jarvis heard you"
                resetListeningState(false)
                transitionHandsFree(HandsFreeState.LISTENING, "wake_detected")
                if (turn.busy) {
                    turn.cancel()
                    handler.postDelayed({ if (!isDestroyed && !turn.busy && !audio.busy) turn.start() }, 350)
                } else turn.start()
            }
            val configured = BuildConfig.OPENAI_API_KEY.isNotBlank()
            val connection = if (configured) "Direct Realtime" else "Needs API key"
            if (state.text.toString() != connection) state.text = connection
            state.setTextColor(if (configured) Color.rgb(114, 220, 182) else Color.rgb(244, 191, 110))
            val description = if (configured) "Ready for a direct voice turn" else "Configure OPENAI_API_KEY in local secrets.properties"
            if (detail.text.toString() != description) detail.text = description
            val wake = prefs.getString("wakeState", "Starting wake listener") ?: "Starting wake listener"
            if (wakeStatus.text.toString() != "●  $wake") wakeStatus.text = "●  $wake"
            val hint = "Direct OpenAI Realtime · push to talk"
            if (modeHint.text.toString() != hint) modeHint.text = hint
            val title = if (turn.recording) "Finish" else "Talk"
            if (talk.text.toString() != title) talk.text = title
            val now = java.text.SimpleDateFormat("h:mm", java.util.Locale.getDefault()).format(java.util.Date())
            findViewById<TextView>(1001)?.text = now
            talk.isEnabled = !audio.busy && (turn.recording || !turn.busy)
            speaker.isEnabled = !audio.busy && !turn.busy
            handler.postDelayed(this, 200)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), dp(12))
            setBackgroundColor(Color.rgb(17, 25, 33))
        }
        wakeBar = View(this).apply { setBackgroundColor(Color.rgb(80, 150, 210)) }
        root.addView(wakeBar, LinearLayout.LayoutParams(-1, dp(4)))
        val home = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        weatherView = TextView(this).apply {
            text = "☾  --°"
            textSize = 22f; setTextColor(Color.rgb(112, 190, 245)); typeface = Typeface.DEFAULT_BOLD
        }
        home.addView(weatherView, FrameLayout.LayoutParams(-2, dp(44)).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.START; leftMargin = dp(6); topMargin = dp(8) })
        wakeStatus = TextView(this).apply { text = "●  Starting wake listener"; textSize = 12f; setTextColor(Color.rgb(125, 190, 150)) }
        home.addView(wakeStatus, FrameLayout.LayoutParams(-2, dp(30)).apply { gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START; leftMargin = dp(10); bottomMargin = dp(8) })
        val clock = TextView(this).apply {
            id = 1001; text = "--:--"; textSize = 132f; gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            setTextColor(Color.rgb(235, 242, 247)); includeFontPadding = false
        }
        val date = TextView(this).apply { text = java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault()).format(java.util.Date()); textSize = 17f; gravity = android.view.Gravity.CENTER; setTextColor(Color.rgb(145, 190, 220)) }
        val timeColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER; addView(clock); addView(date, LinearLayout.LayoutParams(-1, dp(34))) }
        home.addView(timeColumn, FrameLayout.LayoutParams(-1, -1))
        val settings = Button(this).apply { text = "Settings"; textSize = 13f; isAllCaps = false }
        settings.alpha = 0.72f
        home.addView(settings, FrameLayout.LayoutParams(-2, dp(42)).apply { gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL })
        darkButton = Button(this).apply { text = "Dark"; textSize = 12f; isAllCaps = false; alpha = 0.65f }
        darkButton.setOnClickListener {
            darkMode = !darkMode
            val attrs = window.attributes
            attrs.screenBrightness = if (darkMode) 0.03f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = attrs
            darkButton.text = if (darkMode) "Light" else "Dark"
        }
        home.addView(darkButton, FrameLayout.LayoutParams(-2, dp(40)).apply { gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END })
        root.addView(home, LinearLayout.LayoutParams(-1, 0, 1f))
        settingsPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = android.view.View.GONE }
        root.addView(settingsPanel, LinearLayout.LayoutParams(-1, -2))
        fun label(text: String, size: Float) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(Color.rgb(221, 231, 238))
            setPadding(0, dp(2), 0, dp(2)); settingsPanel.addView(this)
        }
        label("Steve  /  Puck 0", 26f)
        label("Voice", 15f)
        val voiceSelector = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, RealtimeVoicePreferences.SUPPORTED)
            setSelection(RealtimeVoicePreferences.SUPPORTED.indexOf(RealtimeVoicePreferences.get(this@MainActivity)))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    RealtimeVoicePreferences.set(this@MainActivity, RealtimeVoicePreferences.SUPPORTED[position])
                }
            }
        }
        settingsPanel.addView(voiceSelector, LinearLayout.LayoutParams(-1, dp(48)))
        state = label("Connecting", 20f)
        detail = label("Ready for a direct voice turn", 13f)
        turnState = label("Ready · Puck", 19f)
        caption = label("Tap Talk, speak, then Finish to send your turn.", 15f)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        settingsPanel.addView(row)
        fun button(title: String, action: () -> Unit): Button = Button(this).apply {
            text = title; textSize = 14f; isAllCaps = false
            row.addView(this, LinearLayout.LayoutParams(0, dp(48), 1f))
            setOnClickListener { action() }
        }
        audio = LocalAudio(applicationContext) { message -> runOnUiThread { if (!isDestroyed) caption.text = message } }
        turn = MediaTurn(applicationContext, { status, message -> runOnUiThread {
            if (!isDestroyed) {
                turnState.text = status; caption.text = message
                if (status.startsWith("Ready") || status.startsWith("Error") || status.startsWith("Failed") || status.contains("failed", true) || status.contains("error", true) || status.contains("timed out", true)) {
                    if (status.startsWith("Ready")) wakeStatus.text = "●  Wake word ready"
                    else wakeStatus.text = "●  Wake word ready"
                } else if (status != "Listening") wakeStatus.text = "●  Steve is responding"
            }
        } }, handsFreeLifecycle)
        talk = button("Talk") {
            if (turn.recording) turn.finish()
            else if (turn.busy) { /* wait for response/playback to finish */ }
            else if (!audio.busy) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) turn.start()
                else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 42)
            }
        }
        talk.visibility = View.VISIBLE
        button("Stop") { turn.cancel(); audio.stop() }
        row.getChildAt(row.childCount - 1).visibility = View.GONE
        button("Clear Conversation") { turn.clearConversation(); caption.text = "Conversation cleared" }
        speaker = button("Test speaker") { if (!turn.busy) audio.tone() }
        button("Record wake sample") { if (!turn.busy && !audio.busy) audio.record() }
        root.addView(Button(this).apply { text = "Test direct Realtime text"; isAllCaps = false; setOnTouchListener { _, event -> if (event.action == android.view.MotionEvent.ACTION_UP) android.util.Log.i("RealtimeProbe", "BUTTON TOUCH UP"); false }; setOnClickListener { android.util.Log.i("RealtimeProbe", "BUTTON CLICK RECEIVED"); Toast.makeText(this@MainActivity, "Realtime probe button pressed", Toast.LENGTH_SHORT).show(); caption.text = "Realtime probe button pressed"; Thread { try { RealtimeWebSocketProbe.run() } catch(e:Exception) { android.util.Log.e("RealtimeProbe", "probe_start_failed ${e.message}") } }.start() } })
        root.addView(Button(this).apply { text = "Test direct Realtime audio"; isAllCaps = false; setOnClickListener { android.util.Log.i("RealtimeAudioProbe", "BUTTON CLICK RECEIVED"); Thread { RealtimeAudioProbe.run(this@MainActivity) }.start() } })
        button("Diag source: VC") { SharedAudioCapture.setDiagnosticSource("vc"); caption.text = "Diagnostic source set to VOICE_COMMUNICATION; restart PuckService to apply." }
        button("Diag WAV start") { SharedAudioCapture.beginDiagnosticWav("capture.wav"); caption.text = "Diagnostic WAV capture started." }
        button("Diag WAV stop") { SharedAudioCapture.endDiagnosticWav(); caption.text = "Diagnostic WAV capture finalized." }
        button("Diag speaker 10s") { if (!turn.busy) audio.diagnosticTone() }
        modeHint = label("Direct OpenAI Realtime · push to talk", 12f)
        settings.setOnClickListener { settingsPanel.visibility = android.view.View.VISIBLE; home.visibility = android.view.View.GONE }
        val back = Button(this).apply { text = "Back to clock"; isAllCaps = false; setOnClickListener { settingsPanel.visibility = android.view.View.GONE; home.visibility = android.view.View.VISIBLE } }
        settingsPanel.addView(back, 0)
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
        ContextCompat.startForegroundService(this, Intent(this, PuckService::class.java))
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 42) {
            if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED && visible) turn.start()
            else caption.text = "Microphone access was not granted. Tap Talk to try again."
        }
    }
    override fun onStart() { super.onStart(); visible = true; SharedAudioCapture.subscribe(handsFreeListener); handler.post(refresh); registerReceiver(wakeReceiver, IntentFilter("com.steve.puckd.WAKE"), RECEIVER_NOT_EXPORTED) }
    override fun onStop() { visible = false; handler.removeCallbacks(refresh); unregisterReceiver(wakeReceiver); SharedAudioCapture.unsubscribe(handsFreeListener); turn.cancel(); audio.stop(); super.onStop() }
    override fun onDestroy() { turn.close(); audio.close(); super.onDestroy() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun transitionHandsFree(next: HandsFreeState, reason: String) {
        if (handsFreeState == next) return
        val previous = handsFreeState
        handsFreeState = next
        android.util.Log.i("HandsFree", "turn_state $previous -> $next reason=$reason")
    }

    private fun resetListeningState() {
        eosFired = false; speechStarted = false; silenceMs = 0; listenStartedAt = SystemClock.elapsedRealtime()
        noiseCalibration.clear(); noiseFloor = 0; speechThreshold = 400; listeningFrames = 0
    }

    private fun analyzeHandsFreeAudio(frame: ShortArray) {
        synchronized(preroll) { preroll.addAll(frame.toList()); while (preroll.size > prerollMaxSamples) preroll.removeFirst() }
        if (handsFreeState != HandsFreeState.LISTENING) return
        if (!firstListeningPcmLogged) { firstListeningPcmLogged = true; if (bargeStartedAt != 0L) android.util.Log.i("HandsFree", "barge_t+${SystemClock.elapsedRealtime()-bargeStartedAt}ms first_pcm") }
        var sum = 0.0
        frame.forEach { sum += it.toDouble() * it.toDouble() }
        val rms = kotlin.math.sqrt(sum / frame.size).toInt()
        val now = SystemClock.elapsedRealtime()
        listeningFrames++
        if (!speechStarted && now - listenStartedAt <= 400L && noiseFloor == 0) noiseCalibration.add(rms)
        if (!speechStarted && now - listenStartedAt >= 400L && noiseFloor == 0) {
            val sorted = noiseCalibration.sorted()
            noiseFloor = if (sorted.isEmpty()) 1 else sorted[sorted.size / 2]
            speechThreshold = maxOf(400, (noiseFloor * 3.0).toInt())
            lastKnownNoiseFloor = noiseFloor
            android.util.Log.i("HandsFree", "eos_noise_floor=$noiseFloor eos_speech_threshold=$speechThreshold")
        }
        if (now - lastRmsLogAt >= 150L) { android.util.Log.i("HandsFree", "speech_rms=$rms speech_started=$speechStarted silence_ms=$silenceMs"); lastRmsLogAt = now }
        val activeThreshold = if (speechStarted) maxOf(250, (speechThreshold * 0.65).toInt()) else speechThreshold
        if (rms >= activeThreshold) {
            if (!speechStarted) { speechStarted = true; android.util.Log.i("HandsFree", "speech_started rms=$rms${if (bargeStartedAt != 0L) " barge_t+${now-bargeStartedAt}ms" else ""}") }
            silenceMs = 0
        } else if (speechStarted) {
            silenceMs += 20
            if (silenceMs % 100L == 0L) android.util.Log.i("HandsFree", "speech_rms=$rms silence_ms=$silenceMs")
            if (silenceMs >= silenceEndMs) finishHandsFree("sustained_silence")
        }
        if (now - listenStartedAt >= maxUtteranceMs) {
            if (speechStarted) finishHandsFree("max_utterance_timeout")
            else finishWithoutSpeech()
        }
    }

    private fun resetListeningState(inheritNoiseFloor: Boolean) {
        eosFired = false; speechStarted = false; silenceMs = 0; listenStartedAt = SystemClock.elapsedRealtime()
        noiseCalibration.clear(); firstListeningPcmLogged = false
        noiseFloor = if (inheritNoiseFloor) lastKnownNoiseFloor else 0
        speechThreshold = if (noiseFloor > 0) maxOf(400, (noiseFloor * 3.0).toInt()) else 400
        listeningFrames = 0
        if (inheritNoiseFloor) android.util.Log.i("HandsFree", "barge_inherited_noise_floor=$noiseFloor eos_speech_threshold=$speechThreshold")
    }

    private fun finishHandsFree(reason: String) {
        if (eosFired || handsFreeState != HandsFreeState.LISTENING) return
        eosFired = true
        android.util.Log.i("HandsFree", "end_of_speech reason=$reason")
        transitionHandsFree(HandsFreeState.PROCESSING, "end_of_speech")
        turn.finish()
    }

    private fun finishWithoutSpeech() {
        if (eosFired || handsFreeState != HandsFreeState.LISTENING) return
        eosFired = true
        android.util.Log.i("HandsFree", "end_of_speech reason=no_speech_timeout")
        transitionHandsFree(HandsFreeState.REARMING, "no_speech_timeout")
        turn.cancel()
        beginWakeRearm()
    }

    private fun beginWakeRearm() {
        android.util.Log.i("HandsFree", "wake_rearm_begin")
        val guardMs = 1200L
        getSharedPreferences("puck_status", 0).edit().putBoolean("wakeArmed", false).apply()
        android.util.Log.i("HandsFree", "wake_guard_ms=$guardMs")
        handler.postDelayed({
            getSharedPreferences("puck_status", 0).edit().putBoolean("wakeArmed", true).apply()
            android.util.Log.i("HandsFree", "wake_subscription_active")
            transitionHandsFree(HandsFreeState.WAKE_ARMED, "wake_subscription_active")
        }, guardMs)
    }
}
