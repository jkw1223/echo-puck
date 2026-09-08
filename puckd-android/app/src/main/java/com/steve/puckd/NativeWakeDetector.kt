package com.steve.puckd

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NativeWakeDetector(context: Context, private val onScore:(Float)->Unit, private val onWake:()->Unit) {
    private val mel=NativeMelFeatures(); private val emb=Interpreter(load(context,"embedding_model.tflite")); private val ww=Interpreter(load(context,"hey_jarvis.tflite"))
    private val melFrames=ArrayDeque<FloatArray>(); private val embeddings=ArrayDeque<FloatArray>(); private var samples=ShortArray(0)
    private fun load(c:Context,n:String):ByteBuffer { val b=c.assets.open("wakeword/$n").use{it.readBytes()}; return ByteBuffer.allocateDirect(b.size).order(ByteOrder.nativeOrder()).put(b).apply{rewind()} }
    @Synchronized fun accept(pcm:ShortArray) { samples= samples+pcm; var off=0; while(samples.size-off>=1760){ val f=mel.frame(samples,off); melFrames.addLast(f); off+=1280; if(melFrames.size>=76){ val x=Array(1){Array(76){FloatArray(32)}}; melFrames.takeLast(76).forEachIndexed{j,v->x[0][j]=v}; val y=Array(1){Array(1){Array(1){FloatArray(96)}}}; emb.run(x,y); embeddings.addLast(y[0][0][0]); while(embeddings.size>=16){ val z=Array(1){Array(16){FloatArray(96)}}; embeddings.take(16).forEachIndexed{j,v->z[0][j]=v}; val out=Array(1){FloatArray(1)}; ww.run(z,out); val score=out[0][0]; onScore(score); if(score>=0.5f){ embeddings.clear(); onWake(); return }; embeddings.removeFirst() } } }; samples=samples.copyOfRange(off,samples.size) }
    fun close(){emb.close();ww.close()}
}
