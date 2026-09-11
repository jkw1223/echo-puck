# `audio_stream_in` effect path

The recovered `audio_stream_in` table is at `0x000f19a0`; `add_audio_effect` is `0x57b44`, `remove_audio_effect` is `0x57e38`, and `set_parameters` is `0x575e0`.

`addAudioEffect` calls the supplied effect interface descriptor getter, rejects duplicates, and stores the interface in the stream arrays at offsets `+0x150` and `+0x118` (maximum three). On the AEC UUID match it calls the stream vtable entry `+0x48` when the stream field at `+0x28` is null, sets byte/field `+0x114` to one, then marks the stream effect state at `+0x10c`. `removeAudioEffect` performs the symmetric UUID check, invokes the same vtable entry when appropriate, and clears `+0x114`.

The decompiler does not show a direct call from this stream method to `AudioPreProcess::addAudioEffect`; that bridge remains an unresolved static edge.
