# AEC state transition

Proven transitions:

1. Android-visible effect attachment reaches the `audio_stream_in` `add_audio_effect` slot at `0x57b44` and recognizes the standard AEC UUID.
2. The preprocess layer at `0x8b734` recognizes the same UUID, sets its AEC flag (`this+4`), creates an echo reference if absent, and configures reverse processing.
3. Reference samples are forwarded through `WriteEchoRefData` and `SPELayer::WriteReferenceBuffer`.

`AudioALSAHardware::SetAudioCommand` at `0x90fa4` has a separate command `0xa4` that directly calls `AudioSpeechEnhanceInfo::SetForceAECRec(param_2 != 0)`. The caller XREF recovered in Phase 7 is `0x912e0`. The string branch `AECOn, need reopen the capture handle` and its exact reopen transition were not fully reconstructed here. The stream-to-preprocess bridge and the effect-registration path remain the stopping boundary.
