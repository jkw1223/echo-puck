# Lineage AEC registration gap

The current XML has no `<library name="pre_processing" ...>` and no AEC effect declaration. Its comments show the intended AOSP entries, including implementation UUID `bb392ec0-8d4d-11e0-a896-0002a5d5c51b`, but those entries are commented out. AudioFlinger reports the XML loaded successfully and lists no preprocessing library. This is direct evidence for the framework registration gap explaining `AcousticEchoCanceler.isAvailable()==false`.
