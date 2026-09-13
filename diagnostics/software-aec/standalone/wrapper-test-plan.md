# Deterministic wrapper validation plan

The native wrapper ABI is compiled and linked into `libsteve_apm.so`. The
following fixture cases are specified for the next executable/harness:

1. null handle/input/output and zero/wrong frame count: expect explicit errors;
2. 16 kHz mono 160-sample zero frame: expect success and all-zero output;
3. deterministic 440 Hz, 16-bit sine fixture: expect finite output and no
   frame-size drift;
4. render-first then capture: render is a delayed/attenuated copy injected into
   capture; compare squared residual energy before/after a 200-frame fixture;
5. delay setter: values below 0 and above 500 return APM's warning status and
   clamp according to upstream semantics.

The shared library's AEC3 processing was built, but this host turn has not
claimed an acoustic reduction number: the Android ARM binary cannot execute on
x86_64. A device-independent fixture executable should be added using the same
source/API before live integration. JNI remains fixture-only and must not use
production capture/playback.
