# Phase 10 report

Phase 10 resolved the most concrete part of boundary A: the stream vtable entry at `+0x48` is `AudioALSAStreamIn::set` at `0x56aec`. It is a stream configuration/lifecycle method, not an effect-forwarding method. Its recovered body contains no call to `AudioPreProcess::addAudioEffect`, `removeAudioEffect`, `start_echo_reference`, or `SetForceAECRec`, and the effect pointer is not passed through this callback.

The preprocess add/remove functions have direct callers in the surrounding preprocess setup region (`0x8b258`, `0x8b2e0`, `0x8b0dc`, `0x8b160`). This is a stronger lead than the prior stream-vtable hypothesis, but the owning object and Android-visible entry into that setup path remain unresolved.

Boundary B could not be inspected live because `adb` is unavailable on the host. No runtime experiment, effect activation, HAL patch, force-AEC command, or ASP call was performed.
