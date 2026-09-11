# Echo-reference origin

Ghidra confirms the `AudioALSACaptureDataClient::copyEchoRefCaptureDataToClient` function and the two call references to `SPELayer::WriteReferenceBuffer` at `0x0009fc60` and `0x0009fca4`. The immediate capture-data/ring-buffer bridge is **PROVEN**. The original producer of the PCM remains **UNKNOWN**: the current project does not provide decompiler data-flow through the ring-buffer fields far enough to distinguish speaker output, ALSA loopback, framework echo-reference, or vendor downlink DMA.
