# Effect UUID map

Both `AudioALSAStreamIn::addAudioEffect` (0x57b44) and `AudioPreProcess::addAudioEffect` (0x8b734) obtain an effect descriptor through the effect interface vtable at `+0x08`, then compare its first 16 bytes with a constant using `memcmp`.

The constant is present at virtual addresses `0x000ec61c` and `0x000ef3d8` as raw bytes `6014497b4d8de01161bd0002a5d5c51b`. Interpreting Android `effect_uuid_t` fields little-endian gives `7b491460-8d4d-11e0-bd61-0002a5d5c51b`, the standard Android Acoustic Echo Canceler type UUID. This identification is proven for the preprocess comparison at `0x8b734` and independently present for the stream comparison.
