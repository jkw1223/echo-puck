# Mode and parameter routing

The device table is at `0x000f44bc`: `setMode` `0x8d6f8`, `setParameters` `0x8d778`, and `openInputStream` `0x90830`. `AudioALSAHardware::setMode` only logs and forwards the integer to `AudioALSAStreamManager::setMode`; no AEC call is present in this wrapper. The recovered stream `setParameters` parses two `AudioParameter` integer keys (the key strings are constructed through relocated string pointers) and routes input-device changes through `AudioALSAStreamManager::routingInputDevice`. The decompiled Phase 8 log is the authoritative record for these relocations.

The requested continuous mode-3/source-7 proof is not complete in this phase: no claim is made that `AUDIO_MODE_IN_COMMUNICATION` alone activates AEC.
