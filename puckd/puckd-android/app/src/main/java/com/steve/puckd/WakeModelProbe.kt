package com.steve.puckd
import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
object WakeModelProbe {
 fun inspect(c: Context) { listOf("melspectrogram.tflite","embedding_model.tflite","hey_jarvis.tflite").forEach { n -> try {
  val b=c.assets.open("wakeword/$n").use { it.readBytes() }; val bb=ByteBuffer.allocateDirect(b.size).order(ByteOrder.nativeOrder()); bb.put(b).rewind(); val i=Interpreter(bb)
  val ins=(0 until i.inputTensorCount).joinToString { "${i.getInputTensor(it).shape().contentToString()} ${i.getInputTensor(it).dataType()}" }; val outs=(0 until i.outputTensorCount).joinToString { "${i.getOutputTensor(it).shape().contentToString()} ${i.getOutputTensor(it).dataType()}" }
  Log.i("WakeModel", "$n inputs=[$ins] outputs=[$outs] bytes=${b.size}"); i.close()
 } catch(e:Exception){Log.e("WakeModel", "$n: ${e.message}")} } }
}
