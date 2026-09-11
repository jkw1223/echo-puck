# Installed effect configuration

The retained workspace contains no captured `/vendor/etc/audio_effects.xml`, `/system/etc/audio_effects.xml`, `/odm/etc/audio_effects.xml`, or legacy `audio_effects.conf` files. The collection inventory records audio configuration paths, but it does not preserve an effect-factory configuration snapshot. Therefore the current Lineage registration state and the reason `AcousticEchoCanceler.isAvailable()` is false cannot be established from retained files alone.
