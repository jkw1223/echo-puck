# Phase 14 timeline

The preserved Phase 14 log establishes:

```text
22:06:42.898  AecDiag event=016 STEP=create_aec null=false enabled=false control=true
22:06:45.689  HIDL audio service SIGSEGV in Device::debug; tombstone_21
22:06:45.997  tombstone written; binder death notifications follow
22:06:58.698  AecDiag event=017 STEP=enable_aec status=-7 before=false after=false control=false
```

The exact shell timestamp of the intervening post-create `dumpsys` command was not preserved in the host transcript, but the HIDL tombstone stack and `AudioALSAHardware: dumpState()` lines show that the service was handling a debug/dump transaction. Phase 16 independently reproduced the same failure with one production `dumpsys media.audio_flinger`, proving the causal confounder.
