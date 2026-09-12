# AEC implementation search

The live ROM contains:

```text
/system/vendor/lib/soundfx/libaudiopreprocessing.so
/system/vendor/lib/libwebrtc_audio_preprocessing.so
/system/lib/libeffectsconfig.so
```

`libaudiopreprocessing.so` is an ARM ELF and contains AEC/preprocessing implementation symbols and diagnostics. However, the running AudioFlinger dump lists no `pre_processing` library, and `/system/vendor/etc/audio_effects.xml` does not declare the `pre_processing` library or AEC effect. This proves an AEC-capable implementation is present but not registered/loaded by the current effect configuration.
