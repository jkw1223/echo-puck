# Echo-reference producer

`AudioALSACaptureDataClient::copyEchoRefCaptureDataToClient` calls `SPELayer::WriteReferenceBuffer` with an `InBufferInfo` derived from ring-buffer/client fields. This is **STRONGLY SUPPORTED** as the immediate bridge. The original PCM producer and any resampling, delay, or timestamp transformation remain **UNKNOWN**.
