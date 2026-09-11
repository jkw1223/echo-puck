# Phase 8 report — ARM-aware AEC control-flow reconstruction

## Result

Phase 8 substantially narrows the edge. Ghidra 12.1.2 successfully decompiled the ARM HAL and proves the platform AEC UUID, stream effect handling, preprocess activation, echo-reference construction, and reference-buffer producer. The phase success criterion is **not yet met**: one continuous Android-visible chain into `start_echo_reference()`/`SetForceAECRec(true)` is still missing because the direct stream-to-preprocess/effect-registration bridge and the exact `AECOn` reopen branch were not recovered.

## Proven graph

```text
framework audio_stream_in.add_audio_effect
  -> AudioALSAStreamIn::addAudioEffect (0x57b44)
  -> descriptor UUID comparison: 7b491460-8d4d-11e0-bd61-0002a5d5c51b
  -> stream state +0x114 and stream vtable +0x48

(preprocess effect attachment, bridge unresolved)
  -> AudioPreProcess::addAudioEffect (0x8b734)
  -> AEC flag this+4 = 1
  -> start_echo_reference (0x8ba50)
  -> create_echo_reference(..., this+0x48)
  -> in_configure_reverse

capture/reference path
  -> copyEchoRefCaptureDataToClient (0x9f830)
  -> AudioPreProcess::WriteEchoRefData
  -> SPELayer::WriteReferenceBuffer (0x84a90)
```

The independent command path is `AudioALSAHardware::SetAudioCommand(0xa4, on)` -> `AudioSpeechEnhanceInfo::SetForceAECRec(on)` at `0x912e0`; its relationship to ordinary framework effect attachment remains unresolved. No runtime experiment was run.
