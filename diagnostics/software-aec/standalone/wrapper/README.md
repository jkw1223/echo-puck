# SteveApm C/JNI boundary

`steve_apm.h` is the only public native header. `SteveApm` is opaque; all
WebRTC and STL types remain private to `steve_apm.cc`. The implementation
returns explicit negative errors for null handles/buffers, unsupported format,
and wrong frame sizes. It catches construction exceptions and never lets C++
exceptions cross the ABI.

The wrapper uses APM's native `StreamConfig` and requires exactly
`AudioProcessing::GetFrameSize(rate)` frames per call (160 at 16 kHz). Render
frames are passed to `ProcessReverseStream` first and copied into an internal
scratch destination. Capture frames then pass through `ProcessStream` into the
caller output. Calls are expected to be serialized by the caller.

`steve_apm_jni.cc` is intentionally thin and uses direct `ByteBuffer` addresses;
it performs no DSP logic. A later Android harness should validate buffer capacity
before calling these methods and retain the `jlong` handle only until destroy.
JNI source is supplied for the Android module; it is not linked into the current
native Meson target because the host-only source build has no Java/Kotlin module.
