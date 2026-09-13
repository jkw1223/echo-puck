#include <jni.h>
#include "steve_apm.h"
extern "C" JNIEXPORT jlong JNICALL Java_com_steve_apmdiag_SteveApm_nativeCreate(JNIEnv*,jclass,jint rate,jint channels){return reinterpret_cast<jlong>(steve_apm_create(rate,channels));}
extern "C" JNIEXPORT void JNICALL Java_com_steve_apmdiag_SteveApm_nativeDestroy(JNIEnv*,jclass,jlong h){steve_apm_destroy(reinterpret_cast<SteveApm*>(h));}
extern "C" JNIEXPORT jint JNICALL Java_com_steve_apmdiag_SteveApm_nativeProcessReverse(JNIEnv* e,jclass,jlong h,jobject b,jint frames){return (!b)?STEVE_APM_INVALID_ARGUMENT:steve_apm_process_reverse(reinterpret_cast<SteveApm*>(h),static_cast<const int16_t*>(e->GetDirectBufferAddress(b)),frames);}
extern "C" JNIEXPORT jint JNICALL Java_com_steve_apmdiag_SteveApm_nativeProcessCapture(JNIEnv* e,jclass,jlong h,jobject in,jobject out,jint frames){return (!in||!out)?STEVE_APM_INVALID_ARGUMENT:steve_apm_process_capture(reinterpret_cast<SteveApm*>(h),static_cast<const int16_t*>(e->GetDirectBufferAddress(in)),static_cast<int16_t*>(e->GetDirectBufferAddress(out)),frames);}
extern "C" JNIEXPORT jint JNICALL Java_com_steve_apmdiag_SteveApm_nativeSetStreamDelay(JNIEnv*,jclass,jlong h,jint d){return steve_apm_set_stream_delay_ms(reinterpret_cast<SteveApm*>(h),d);}
