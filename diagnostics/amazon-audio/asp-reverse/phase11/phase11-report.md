# Phase 11 report

Phase 11 resolves the principal static gap inside the HAL. The calls at `0x8b0dc`, `0x8b160`, `0x8b258`, and `0x8b2e0` all belong to `AudioPreProcess::CheckNativeEffect()` at `0x8b050`. This function reconciles the current preprocess effect list (`this+8 + index*0xc`) with a configuration-owned effect pointer array at `*(this+0x5c) + 0xe8 + index*4`. It removes stale effects and adds missing configured effects, calling the proven preprocess add/remove methods.

This establishes the internal synchronization step:

```text
configuration effect array
  -> AudioPreProcess::CheckNativeEffect
  -> AudioPreProcess::addAudioEffect
  -> AEC UUID recognition
  -> AEC state / echo-reference creation
```

It does not prove that `AudioALSAStreamIn::addAudioEffect` directly populates that configuration array. The stream branch writes `AudioALSAStreamIn+0x114`, while the preprocess synchronizer consumes its own `this+0x5c` configuration object. The event that invokes `CheckNativeEffect`, all readers of stream `+0x114`, the exact capture-reopen condition, and the external owner of `AudioPreProcess` remain unresolved.

The runtime half could not run because `adb` is not installed on the analysis host. No AEC activation, framework effect creation, HAL patch, `SetAudioCommand(0xa4, ...)`, mixer change, or ASP call was performed.
