package com.steve.puckd

import android.media.*
import android.util.Log
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

    fun start() { if (running) return; running = true; exec.execute { loop() } }
    fun subscribe(listener: (ShortArray)->Unit) { listeners.add(listener); start() }
    fun unsubscribe(listener: (ShortArray)->Unit) { listeners.remove(listener) }
    fun beginTurn() { synchronized(turn) { turn.clear() }; finishRequested = false; activeTurn = true; start() }
    fun requestFinish() { finishRequested = true }
    fun isFinishRequested() = finishRequested
    fun endTurn(): ByteArray { activeTurn = false; synchronized(turn) { val out=ByteArray(turn.size*2); turn.forEachIndexed { i,v -> out[i*2]=v.toByte(); out[i*2+1]=(v.toInt() shr 8).toByte() }; return out } }
    fun stop() { running=false; recorder?.let { try { it.stop() } catch (_:Exception) {}; it.release() }; recorder=null; exec.shutdownNow() }
    private fun loop() { try { val min=AudioRecord.getMinBufferSize(16000,16,2); recorder=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,16,2,maxOf(min*2,6400)); check(recorder!!.state==AudioRecord.STATE_INITIALIZED); recorder!!.startRecording(); Log.i(TAG,"single microphone owner started"); val b=ShortArray(320); while(running){ val n=recorder!!.read(b,0,b.size); if(n>0){ val f=if(n==b.size)b.copyOf() else b.copyOf(n); if (turn.size % 16000 < 320) { var peak=0; f.forEach{peak=maxOf(peak,kotlin.math.abs(it.toInt()))}; Log.i(TAG,"pcm peak=$peak state="+try{java.io.File("/sys/devices/platform/amazon-gating/state").readText().trim()}catch(_:Exception){"?"}) }; if(activeTurn) synchronized(turn){f.forEach{turn.add(it)}}; listeners.forEach{it(f)} } } } catch(e:Exception){Log.e(TAG,"capture failed",e)} }
}
