package com.steve.puckd

import kotlin.math.*

/** Reference mel front-end matching Tower's 16 kHz/400 window/160 hop contract. */
class NativeMelFeatures {
    companion object { const val RATE=16000; const val WINDOW=400; const val HOP=160; const val BANDS=32; const val LOW=60.0; const val HIGH=3800.0 }
    private val window=FloatArray(WINDOW){(0.5-0.5*cos(2.0*PI*it/(WINDOW-1))).toFloat()}
    private fun hzMel(h:Double)=2595.0*log10(1+h/700.0)
    private val filters=run {
        val edges=IntArray(BANDS+2){ i -> floor((WINDOW+1)*((700*(10.0.pow((hzMel(LOW)+(hzMel(HIGH)-hzMel(LOW))*i/(BANDS+1))/2595.0)-1))/RATE)).toInt() }
        Array(BANDS){ b -> FloatArray(WINDOW/2+1){ k -> when { k<edges[b]||k>edges[b+2]->0f; k<=edges[b+1]->((k-edges[b]).toFloat()/max(1,edges[b+1]-edges[b])); else->((edges[b+2]-k).toFloat()/max(1,edges[b+2]-edges[b+1])) } } }
    }
    fun frame(pcm: ShortArray, offset:Int): FloatArray {
        val power=DoubleArray(WINDOW/2+1); for(k in power.indices){ var re=0.0; var im=0.0; for(n in 0 until WINDOW){ val x=if(offset+n<pcm.size) pcm[offset+n].toDouble()*window[n] else 0.0; val a=2*PI*k*n/WINDOW; re+=x*cos(a); im-=x*sin(a) }; power[k]=(re*re+im*im)/WINDOW }
        return FloatArray(BANDS){ b -> ln((filters[b].indices.sumOf { (power[it]*filters[b][it]).toDouble() }).coerceAtLeast(1e-10)).toFloat() }
    }
}
