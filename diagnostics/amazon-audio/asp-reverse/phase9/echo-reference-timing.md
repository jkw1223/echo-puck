# Echo-reference timing

`AudioPreProcess::addAudioEffect` calls `start_echo_reference` immediately when the AEC UUID matches and `this+0x48` is null. `start_echo_reference` calls `create_echo_reference` and stores the interface. The later PCM producer path copies reference data and feeds `WriteEchoRefData`/`WriteReferenceBuffer`. Whether usable playback PCM exists at attachment time, and whether another path restarts or refreshes the reference when output becomes active, is UNKNOWN.
