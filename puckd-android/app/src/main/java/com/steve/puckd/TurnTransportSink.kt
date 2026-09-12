package com.steve.puckd

data class TurnAudioFormat(val sampleRate: Int, val channels: Int, val encoding: Int)

interface TurnTransportSink {
    fun onResponseAudio(pcm: ByteArray, format: TurnAudioFormat)
    fun onResponseText(textDelta: String) {}
    fun onResponseCompleted() {}
    fun onTransportError(error: Throwable) {}
    fun onResponseAudioStarted() {}
}
