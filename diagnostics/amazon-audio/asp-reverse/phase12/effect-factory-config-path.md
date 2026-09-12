# Effect-factory configuration path

Live `libeffectsconfig.so` string inspection recovered these search-path strings:

```text
/vendor/etc/audio/sku_
/system/etc
/vendor/etc
/vendor/etc/audio
```

It also contains `audio_effects.xml`, `audio_effects_conf`, `preprocess`, and parser diagnostics. The running ROM exposes both `/system/etc/audio_effects.conf` and `/system/vendor/etc/audio_effects.xml`; the XML is the configuration loaded by AudioFlinger in the captured dump.
