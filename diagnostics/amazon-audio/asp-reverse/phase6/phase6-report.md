# Phase 6 ARM control-flow reconstruction

## Scope and tooling boundary

This pass remained static. No vendor binary, mixer control, proprietary ASP function, production APK, or device state was changed. Ghidra/rizin was not available at the start of the pass; a Homebrew `rizin` installation was attempted but did not complete on the Intel host before the bounded analysis window. The evidence below uses the 32-bit ARM symbols, relocations, ARM disassembly, PLT call sites, and string data already captured under `asp-reverse/`.

## Result

Phase 6 completion is **not reached**. The current evidence does not establish either requested continuous Android-visible activation chain. The unresolved edges are explicitly listed below.

## Confirmed control-flow anchors

```text
AudioALSAHardware::setMode(int)                         0x8d6f8
AudioALSAHardware::setParameters(String8 const&)        0x8d778
AudioALSAHardware::SetAudioCommand(int,int)             0x90fa4
AudioALSAStreamManager::setMode(audio_mode_t)            0x5b6a4
AudioSpeechEnhanceInfo::SetForceAECRec(bool)             0x7d958
AudioPreProcess::start_echo_reference(...)               0x8ba50
SPELayer::WriteReferenceBuffer(InBufferInfo*)            0x84a90
```

`SetAudioCommand` has one direct relocation-visible call to `SetForceAECRec`. The second argument is converted to a Boolean. The command strings identify the branch as `SET_AECREC_TEST_ENABLE`, but the Android-visible caller or external mapping is not proven.

`AudioALSAHardware::setParameters` constructs `AudioParameter` and calls `getFloat`, `getInt`, and `remove`. The exported standard key objects include routing, sampling rate, format, channels, frame count, input source, and screen state. No exact AEC/ASP vendor key was recovered.

`AudioPreProcess::start_echo_reference` calls the external `create_echo_reference` symbol after clearing prior reference state. Its visible caller path is inside preprocessing/effect handling, where an effect descriptor is compared before the call. The descriptor bytes/UUID are not yet decoded.

`WriteReferenceBuffer` has direct call sites from `AudioALSACaptureDataClient::copyEchoRefCaptureDataToClient`, but the original producer of the ring-buffer PCM is not proven to be speaker output, ALSA loopback, or AudioFlinger echo reference.

## Required deliverables and status

| Deliverable | Status |
|---|---|
| `effect-aec-path.md` | unresolved descriptor bytes and Android callback hop |
| `audio-stream-in-table.md` | function table not recovered from stripped binary |
| `audio-hw-device-table.md` | function table not recovered from stripped binary |
| `echo-reference-producer.md` | ring-buffer bridge proven; original producer unresolved |
| `aec-reopen-state.md` | log branch located; complete old/new state transition unresolved |
| `set-audio-command-callers.md` | internal caller proven; external reachability unresolved |
| `parameter-map.md` | standard AudioParameter keys identified; AEC key not proven |

The companion files use the same evidence/confidence labels and preserve the unresolved edge rather than presenting an inference as a fact.

## Decision boundary

Do not run the `MODE_IN_COMMUNICATION` experiment, send `SET_AECREC_TEST_ENABLE`, attach an effect, or invoke an ASP API yet. The missing proof is the Android-facing function-table/effect descriptor path and the exact reference producer.
