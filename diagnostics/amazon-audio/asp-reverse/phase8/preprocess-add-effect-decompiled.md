# `AudioPreProcess::addAudioEffect`

At `0x8b734`, the method locks `this+0x4c`, obtains the effect descriptor through vtable `+0x08`, prevents duplicates, and stores up to three effect interfaces at `this+8 + index*0xc`. For the AEC UUID `7b491460-8d4d-11e0-bd61-0002a5d5c51b`, it sets `this[4]=1`. If `this+0x48` is null it calls `start_echo_reference(this, ..., config+0x18, config+0x1c)`, stores the returned echo-reference interface at `+0x48`, and then calls `in_configure_reverse` with the configured rate/format. This is the strongest static AEC activation evidence.
