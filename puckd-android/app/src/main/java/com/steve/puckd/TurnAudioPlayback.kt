package com.steve.puckd

import android.media.*
import android.os.SystemClock
import android.util.Log

/** Shared 16 kHz mono PCM speaker playback for turn transports. */
class TurnAudioPlayback {
    fun play(pcm: ByteArray, checkActive: () -> Unit) {
        checkActive()
        val track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(16000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size).build()
        try {
            check(track.state != AudioTrack.STATE_UNINITIALIZED) { "Speaker unavailable" }
            check(track.write(pcm, 0, pcm.size) == pcm.size) { "Incomplete reply audio write" }
            checkActive(); track.play()
            val frames = pcm.size / 2; val deadline = SystemClock.elapsedRealtime() + frames * 1000L / 16000 + 2000
            while(track.playbackHeadPosition < frames && SystemClock.elapsedRealtime() < deadline) { checkActive(); Thread.sleep(20) }
            checkActive(); check(track.playbackHeadPosition >= frames) { "Reply playback timed out" }
            Log.i(TAG, "playback_complete frames=${track.playbackHeadPosition} expected=$frames")
        } finally { try { track.stop() } finally { track.release() } }
    }
    companion object { private const val TAG = "PuckPlayback" }
}
