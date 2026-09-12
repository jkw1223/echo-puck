# Phase 12 report

Phase 12 recovered the missing internal lifecycle edge and collected read-only runtime evidence.

The static bridge is:

```text
AudioALSACaptureDataClient::CheckNativeEffect (0x9d310)
    -> AudioPreProcess::CheckNativeEffect (this + 0x1b54)
    -> reconcile config+0xe8 effect pointers against preprocess list
    -> AudioPreProcess::addAudioEffect / removeAudioEffect
    -> standard AEC UUID recognition
    -> start_echo_reference when the reference handle is null
```

The configuration object fields are proven as `+0xdc` dirty flag, `+0xe0` effect count, and `+0xe8` configured effect-pointer array. The framework/stream pointer lineage into that array remains unresolved, and the exact caller event that sets the dirty flag remains unresolved.

Runtime evidence is now decisive on registration. The Show is reachable at `192.168.1.24:5555` using Android platform-tools `adb 37.0.1` from `/Users/jason/Library/Android/sdk/platform-tools/adb`. AudioFlinger reports the XML loaded successfully but only loads bundle, reverb, visualizer, downmix, loudness enhancer, and dynamics-processing libraries. The live `/system/vendor/etc/audio_effects.xml` contains no `pre_processing` library and no AEC effect declaration; the AOSP AEC entries are present only as comments. The ROM nevertheless contains `/system/vendor/lib/soundfx/libaudiopreprocessing.so` and `/system/vendor/lib/libwebrtc_audio_preprocessing.so`.

Therefore the current framework availability failure is strongly supported as a registration gap: an AEC-capable implementation is present on the ROM but is not declared/loaded by the active effect configuration. A complete `AudioEffect.queryEffects()` descriptor list was not collected because the diagnostic app query is coupled to its capture-start path and the Show was locked; no effect was instantiated.

No AEC activation, HAL patch, `SetAudioCommand(0xa4, ...)`, mixer change, or ASP call was performed.
