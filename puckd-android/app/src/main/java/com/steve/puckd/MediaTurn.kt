package com.steve.puckd

/** Stable turn lifecycle facade. Protocol and network behavior live in TurnTransport. */
class MediaTurn(context: android.content.Context, report: (String, String) -> Unit) {
    private val playback = TurnAudioPlayback()
    private val sink = object : TurnTransportSink {
        override fun onResponseAudio(pcm: ByteArray, format: TurnAudioFormat) = playback.play(pcm) {}
        override fun onResponseText(textDelta: String) { if (textDelta.isNotBlank()) report("Steve", textDelta) }
    }
    private val transport: TurnTransport = if (context.getSharedPreferences("puck_status", 0)
        .getString("transport", "relay") == "realtime") RealtimeTurnTransport(sink)
    else RelayTurnTransport(context, report, sink)
    val busy get() = transport.busy
    val recording get() = transport.recording
    fun start() = transport.start()
    fun finish() = transport.finish()
    fun cancel() = transport.cancel()
    fun connectionChanged(token: String) = transport.connectionChanged(token)
    fun close() = transport.close()
}
