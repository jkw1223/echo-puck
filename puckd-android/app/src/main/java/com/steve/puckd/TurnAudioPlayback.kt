package com.steve.puckd

import android.media.*
import android.os.SystemClock
import android.util.Log

/** Shared PCM speaker playback for turn transports. */
class TurnAudioPlayback {
    @Volatile private var currentTrack: AudioTrack? = null
    @Volatile private var generation = 0L

    fun cancelCurrentPlayback() {
        generation++
        currentTrack?.let { runCatching { it.pause(); it.flush(); it.stop() } }
        Log.i(TAG, "playback_flushed")
    }

    fun play(pcm: ByteArray, format: TurnAudioFormat, checkActive: () -> Unit) {
        val playGeneration = generation
        checkActive()
        val track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(format.sampleRate)
                .setChannelMask(if (format.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(format.encoding).build())
            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size).build()
        currentTrack = track
        try {
            check(track.state != AudioTrack.STATE_UNINITIALIZED) { "Speaker unavailable" }
            check(track.write(pcm, 0, pcm.size) == pcm.size) { "Incomplete reply audio write" }
            check(playGeneration == generation) { "Playback interrupted" }
            checkActive(); track.play()
            val bytesPerSample = if (format.encoding == AudioFormat.ENCODING_PCM_8BIT) 1 else 2
            val frames = pcm.size / (bytesPerSample * format.channels)
            val deadline = SystemClock.elapsedRealtime() + frames * 1000L / format.sampleRate + 2000
            while(track.playbackHeadPosition < frames && SystemClock.elapsedRealtime() < deadline) {
                check(playGeneration == generation) { "Playback interrupted" }
                checkActive(); Thread.sleep(20)
            }
            check(playGeneration == generation) { "Playback interrupted" }
            checkActive(); check(track.playbackHeadPosition >= frames) { "Reply playback timed out" }
            Log.i(TAG, "playback_complete frames=${track.playbackHeadPosition} expected=$frames")
        } finally { if (currentTrack === track) currentTrack = null; try { track.stop() } finally { track.release() } }
    }
    companion object { private const val TAG = "PuckPlayback" }
}
