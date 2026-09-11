# Echo-reference lifecycle

The lifecycle is implemented by `AudioPreProcess` methods. `start_echo_reference(audio_format_t, channel_count, sampling_rate)` calls the external `create_echo_reference` symbol after clearing prior reference state. The same object exposes add, update, push, stop, clear, remove, delay, and release operations.

The exact creation condition is not proven by this pass. The presence of `AECOn, need reopen the capture handle`, `try start_echo_reference`, and playback/VOIP queue strings indicates that capture/playback state controls lifecycle, but no claim is made that source 7 alone creates it.
