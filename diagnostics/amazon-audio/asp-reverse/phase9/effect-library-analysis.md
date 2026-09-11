# Effect-library analysis

The retained symbol/string/metadata inventories for the Amazon/MediaTek libraries do not expose an `AUDIO_EFFECT_LIBRARY_INFO_SYM`, `create_effect`, `release_effect`, or `query_effect` registration record tied to the standard AEC UUID. The HAL compares the AEC type UUID as a notification/selection signal, but the retained evidence does not identify an Android effect implementation UUID or a standalone plugin library.
