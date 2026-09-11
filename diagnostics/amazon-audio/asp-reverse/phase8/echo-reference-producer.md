# Playback-reference producer

`AudioALSACaptureDataClient::copyEchoRefCaptureDataToClient` at `0x9f830` copies the reference ring buffer, optionally converts it, passes it to `AudioPreProcess::WriteEchoRefData`, and then calls `SPELayer::WriteReferenceBuffer` at `0x84a90`. XREFs to `WriteReferenceBuffer` are at `0x9fc60` and `0x9fca4`. This proves the playback/reference data path into preprocessing and SPE. It does not by itself prove which Android output operation first populates the ring buffer.
