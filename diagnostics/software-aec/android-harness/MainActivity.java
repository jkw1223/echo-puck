package com.steve.apmdiag;
import android.app.*; import android.os.*; import android.util.Log; import java.nio.*;
public class MainActivity extends Activity {
 static { System.loadLibrary("steve_apm_jni"); }
 private static native long nativeCreate(int r,int c); private static native void nativeDestroy(long h);
 private static native int nativeProcessReverse(long h,ByteBuffer b,int f); private static native int nativeProcessCapture(long h,ByteBuffer i,ByteBuffer o,int f); private static native int nativeSetStreamDelay(long h,int d);
 public void onCreate(Bundle b){ super.onCreate(b); new Thread(()->{ int f=160; long h=nativeCreate(16000,1); Log.i("ApmDiag","create="+h); ByteBuffer r=ByteBuffer.allocateDirect(f*2).order(ByteOrder.nativeOrder()),i=ByteBuffer.allocateDirect(f*2).order(ByteOrder.nativeOrder()),o=ByteBuffer.allocateDirect(f*2).order(ByteOrder.nativeOrder()); Log.i("ApmDiag","delay="+nativeSetStreamDelay(h,80)); for(int n=0;n<1200;n++){int a=nativeProcessReverse(h,r,f),c=nativeProcessCapture(h,i,o,f); if(a!=0||c!=0)Log.e("ApmDiag","frame="+n+" reverse="+a+" capture="+c);} nativeDestroy(h); Log.i("ApmDiag","destroyed frames=1200"); }).start(); }
}
