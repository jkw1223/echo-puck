# Reference-buffer path

`SPELayer::WriteReferenceBuffer(InBufferInfo*)` at `0x84a90` locks a buffer path and feeds `AddtoInputBuffer`. `SPELayer::Process_VoIP` consumes the resulting uplink/downlink queues. The HAL strings identify `mDLInBufferQ`, `WriteReferenceBuffer`, `Process_VoIP`, and `push_echo_reference`, establishing an internal downlink/reference queue rather than an Android effect chain.

The current static pass does not recover enough structure-field names to prove whether the original buffer is `out_write` PCM, an ALSA loopback, or a separately supplied downlink stream. Sample-rate/channel conversion and timestamp compensation remain unresolved.
