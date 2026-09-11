package com.steve.puckd

import android.content.Context
import android.media.*
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.*

/** Foreground-only smoke test. Captured audio never enters the relay. */
class LocalAudio(private val context: Context, private val report: (String) -> Unit) {
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var cancelled = false
    @Volatile var busy = false
        private set
    @Volatile private var closed = false
    private var samples: ShortArray? = null
    private val sampleRate = 16000
    fun stop() { cancelled = true }
    fun close() { closed = true; stop(); worker.shutdown() }
    private fun runTask(task: () -> Unit) {
        if (busy || closed) return
        cancelled = false
        busy = true
        worker.execute {
            try { task() }
            catch (e: Exception) { report("Audio test failed: ${e.message}"); Log.e("PuckAudio", "test failed", e) }
            finally { busy = false; if (cancelled) report("Stopped · microphone off") }
        }
    }
    fun tone() = runTask {
        report("Playing a short test tone")
        val tone = ShortArray(sampleRate) { i ->
            val envelope = minOf(1.0, i / 320.0, (sampleRate - i - 1) / 320.0)
            (sin(2.0 * PI * 440 * i / sampleRate) * 4000 * envelope).toInt().toShort()
        }
        play(tone)
        if (!cancelled) report("Tone sent to speaker · did you hear it?")
    }
    fun diagnosticTone(seconds: Int = 10) = runTask {
        val tone = ShortArray(sampleRate * seconds) { i ->
            (sin(2.0 * PI * 440 * i / sampleRate) * 4000).toInt().toShort()
        }
        play(tone)
        if (!cancelled) report("Diagnostic tone completed")
    }
    fun record() = runTask {
        samples = null
        File(context.filesDir, "audio-test.wav").delete()
        report("Recording for 5 seconds · speak now")
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minBuffer > 0) { "16 kHz mono capture unsupported ($minBuffer)" }
        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuffer * 2, 6400))
        try {
            check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone did not initialize" }
            val captured = ShortArray(sampleRate * 5)
            var count = 0
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone did not start" }
            val deadline = SystemClock.elapsedRealtime() + 6500
            while (count < captured.size && !cancelled && SystemClock.elapsedRealtime() < deadline) {
                val read = recorder.read(captured, count, minOf(1600, captured.size - count), AudioRecord.READ_NON_BLOCKING)
                check(read >= 0) { "Microphone read error $read" }
                if (read == 0) Thread.sleep(10) else count += read
            }
            recorder.stop()
            if (cancelled) return@runTask
            check(count == captured.size) { "Capture timed out: $count / ${captured.size} samples" }
            samples = captured
            writeWav(captured)
            val peak = captured.maxOf { abs(it.toInt()) }
            val rms = sqrt(captured.sumOf { it.toDouble() * it } / count)
            val db = if (rms > 0) 20 * log10(rms / 32768) else -120.0
            Log.i("PuckAudio", "capture_complete frames=$count rate=$sampleRate channels=1 peak=$peak rms_dbfs=$db route=${recorder.routedDevice?.type}")
            report("Recorded 5s · level ${db.roundToInt()} dBFS\nTap Play recording to check your voice.")
        } finally { recorder.release() }
    }
    fun replay() = runTask {
        val captured = samples
        if (captured == null) { report("Record a short sample first."); return@runTask }
        report("Playing your recording · microphone off")
        play(captured)
        if (!cancelled) report("Playback complete · was your voice clear?")
    }
    private fun play(pcm: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size * 2).build()
        try {
            check(track.state != AudioTrack.STATE_UNINITIALIZED) { "Speaker did not initialize" }
            check(track.write(pcm, 0, pcm.size) == pcm.size) { "Incomplete speaker write" }
            if (cancelled) return
            track.play()
            val deadline = SystemClock.elapsedRealtime() + (pcm.size * 1000L / sampleRate) + 2000
            while (!cancelled && track.playbackHeadPosition < pcm.size && SystemClock.elapsedRealtime() < deadline) Thread.sleep(20)
            val played = track.playbackHeadPosition
            Log.i("PuckAudio", "playback_complete frames=$played expected=${pcm.size} rate=$sampleRate route=${track.routedDevice?.type} cancelled=$cancelled")
            if (!cancelled) check(played >= pcm.size) { "Playback timed out at $played frames" }
            track.stop()
        } finally { track.release() }
    }
    private fun writeWav(pcm: ShortArray) {
        val bytes = pcm.size * 2
        val buffer = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(sampleRate).putInt(sampleRate * 2)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(bytes)
        pcm.forEach { buffer.putShort(it) }
        File(context.filesDir, "audio-test.wav").writeBytes(buffer.array())
    }
}
