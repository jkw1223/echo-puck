package com.steve.puckd

/** Stable turn lifecycle facade. Protocol and network behavior live in TurnTransport. */
class MediaTurn(context: android.content.Context, report: (String, String) -> Unit, private val lifecycle: TurnTransportSink? = null) {
    private val playback = TurnAudioPlayback()
    private val sink = object : TurnTransportSink {
        override fun onResponseAudio(pcm: ByteArray, format: TurnAudioFormat) {
            lifecycle?.onResponseAudioStarted()
            try {
                playback.play(pcm, format) {}
            } catch (e: IllegalStateException) {
                if (e.message == "Playback interrupted") {
                    android.util.Log.i("PuckPlayback", "playback_interruption_acknowledged")
                } else {
                    lifecycle?.onTransportError(e)
                }
            }
        }
        override fun onResponseCompleted() { lifecycle?.onResponseCompleted() }
        override fun onTransportError(error: Throwable) { lifecycle?.onTransportError(error) }
        override fun onResponseText(textDelta: String) { if (textDelta.isNotBlank()) report("Steve", textDelta) }
    }
    private val transport: TurnTransport = if (context.getSharedPreferences("puck_status", 0)
        .getString("transport", "relay") == "realtime") RealtimeTurnTransport(context, sink)
    else RelayTurnTransport(context, report, sink)
    val busy get() = transport.busy
    val recording get() = transport.recording
    fun start(initialPcm: ShortArray? = null) = transport.start(initialPcm)
    fun finish() = transport.finish()
    fun cancel() = transport.cancel()
    fun interruptCurrentResponse() { transport.cancel(); playback.cancelCurrentPlayback() }
    fun clearConversation() = ConversationHistory.clearConversation()
    fun connectionChanged(token: String) = transport.connectionChanged(token)
    fun close() = transport.close()
}
