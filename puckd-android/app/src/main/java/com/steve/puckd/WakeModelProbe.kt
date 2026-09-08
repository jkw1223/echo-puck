package com.steve.puckd
import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
object WakeModelProbe {
 fun inspect(c: Context) { listOf("melspectrogram.tflite","embedding_model.tflite","hey_jarvis.tflite").forEach { n -> try {
  val b=c.assets.open("wakeword/$n").use { it.readBytes() }; val bb=ByteBuffer.allocateDirect(b.size).order(ByteOrder.nativeOrder()); bb.put(b).rewind(); val i=Interpreter(bb, Interpreter.Options().setUseXNNPACK(false))
  Log.i("WakeModel", "$n inputs=${i.inputTensorCount} outputs=${i.outputTensorCount} in=${i.getInputTensor(0).shape().contentToString()} out=${i.getOutputTensor(0).shape().contentToString()}"); i.close()
 } catch(e:Exception){Log.e("WakeModel", "$n: ${e.message}")} } }
}
