package com.steve.puckd

import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.media.audiofx.AudioEffect
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/** Single microphone owner. Publishes fixed 20ms PCM frames to subscribers and buffers active turns. */
object SharedAudioCapture {
    private const val TAG = "SharedAudio"
    private val exec = Executors.newSingleThreadExecutor()
    private val listeners = CopyOnWriteArrayList<(ShortArray)->Unit>()
    @Volatile private var running = false
    @Volatile private var activeTurn = false
    @Volatile private var finishRequested = false
    private val turn = ArrayList<Short>()
    private var recorder: AudioRecord? = null
    private var wav: RandomAccessFile? = null
    private var wavFrames = 0L
    private var metricSamples = 0L
    private var metricSum = 0.0
    private var metricSquares = 0.0
    private var metricPeak = 0
    private var metricWakeScore = "n/a"
    private val diagnosticDir = File("/data/data/com.steve.puckd/files/amazon-audio")

    private fun sourceName(source: Int) = when (source) {
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
        MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
        MediaRecorder.AudioSource.MIC -> "MIC"
        MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
        else -> "UNKNOWN_$source"
    }

    private fun selectedSource(): Int = when (runCatching { File(diagnosticDir, "source.txt").readText().trim().substringBefore(':') }.getOrDefault("")) {
        "vc" -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
        "mic" -> MediaRecorder.AudioSource.MIC
        "raw" -> MediaRecorder.AudioSource.UNPROCESSED
        else -> MediaRecorder.AudioSource.VOICE_RECOGNITION
    }

    fun setDiagnosticSource(name: String) { diagnosticDir.apply { mkdirs() }; File(diagnosticDir, "source.txt").writeText(name) }
    fun beginDiagnosticWav(name: String) {
        synchronized(this) {
            endDiagnosticWav()
            val dir = diagnosticDir.apply { mkdirs() }
            wav = RandomAccessFile(File(dir, name), "rw").apply { setLength(44); seek(44); wavFrames = 0 }
        }
    }
    fun endDiagnosticWav() {
        synchronized(this) {
            wav?.let { f ->
                f.seek(0); f.writeBytes("RIFF"); f.writeInt(Integer.reverseBytes((36 + wavFrames * 2).toInt())); f.writeBytes("WAVEfmt "); f.writeInt(Integer.reverseBytes(16)); f.writeShort(java.lang.Short.reverseBytes(1).toInt()); f.writeShort(java.lang.Short.reverseBytes(1).toInt()); f.writeInt(Integer.reverseBytes(16000)); f.writeInt(Integer.reverseBytes(32000)); f.writeShort(java.lang.Short.reverseBytes(2).toInt()); f.writeShort(java.lang.Short.reverseBytes(16).toInt()); f.writeBytes("data"); f.writeInt(Integer.reverseBytes((wavFrames * 2).toInt())); f.close()
            }; wav = null
        }
    }

    fun start() { if (running) return; running = true; exec.execute { loop() } }
    fun subscribe(listener: (ShortArray)->Unit) { listeners.add(listener); start() }
    fun unsubscribe(listener: (ShortArray)->Unit) { listeners.remove(listener) }
    fun beginTurn() { synchronized(turn) { turn.clear() }; finishRequested = false; activeTurn = true; start() }
    fun requestFinish() { finishRequested = true }
    fun isFinishRequested() = finishRequested
    fun endTurn(): ByteArray { activeTurn = false; synchronized(turn) { val out=ByteArray(turn.size*2); turn.forEachIndexed { i,v -> out[i*2]=v.toByte(); out[i*2+1]=(v.toInt() shr 8).toByte() }; return out } }
    fun stop() { running=false; endDiagnosticWav(); recorder?.let { try { it.stop() } catch (_:Exception) {}; it.release() }; recorder=null; exec.shutdownNow() }
    private fun loop() { try {
        val config = runCatching { File(diagnosticDir, "source.txt").readText().trim() }.getOrDefault("vr")
        val source = selectedSource(); val min=AudioRecord.getMinBufferSize(16000,16,2)
        config.substringAfter(':', "").takeIf { it.isNotBlank() }?.let { beginDiagnosticWav(it) }
        recorder=AudioRecord(source,16000,16,2,maxOf(min*2,6400)); check(recorder!!.state==AudioRecord.STATE_INITIALIZED); recorder!!.startRecording()
        val route = runCatching { recorder!!.routedDevice }.getOrNull()
        Log.i("PuckAudioDiag", "source=${sourceName(source)} state=${recorder!!.state} recording=${recorder!!.recordingState} session=${recorder!!.audioSessionId} rate=${recorder!!.sampleRate} channels=${recorder!!.channelCount} format=${recorder!!.audioFormat} buffer=${maxOf(min*2,6400)} routeId=${route?.id} routeType=${route?.type} routeName=${route?.productName} aec=${AcousticEchoCanceler.isAvailable()} ns=${NoiseSuppressor.isAvailable()} agc=${AutomaticGainControl.isAvailable()}")
        runCatching { AudioEffect.queryEffects().forEach { d -> Log.i("PuckAudioDiag", "effect name=${d.name} implementor=${d.implementor} type=${d.type} implementation=${d.uuid}") } }.onFailure { Log.e("PuckAudioDiag", "effect query failed", it) }
        Log.i(TAG,"single microphone owner started"); val b=ShortArray(320); while(running){ val n=recorder!!.read(b,0,b.size); if(n>0){ val f=if(n==b.size)b.copyOf() else b.copyOf(n)
            synchronized(this) { wav?.let { file -> f.forEach { file.writeShort(java.lang.Short.reverseBytes(it).toInt()); wavFrames++ } }; f.forEach { v -> val x=v.toDouble(); metricSamples++; metricSum+=x; metricSquares+=x*x; metricPeak=maxOf(metricPeak,kotlin.math.abs(v.toInt())) }; if(metricSamples >= 16000) { val mean=metricSum/metricSamples; val rms=kotlin.math.sqrt(metricSquares/metricSamples); Log.i("PuckAudioDiag", "source=${sourceName(source)} samples=$metricSamples rms=$rms peak=$metricPeak mean=$mean wake_score=$metricWakeScore"); metricSamples=0; metricSum=0.0; metricSquares=0.0; metricPeak=0 } }
            if(activeTurn) synchronized(turn){f.forEach{turn.add(it)}}; listeners.forEach{it(f)} } } } catch(e:Exception){Log.e(TAG,"capture failed",e)} }
}
