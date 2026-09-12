# Capture reopen chain

The exact `AECOn, need reopen the capture handle` branch was not fully resolved. The newly proven `AudioALSACaptureDataClient::CheckNativeEffect` path only synchronizes effect objects and clears `config+0xdc`; it contains no close/open sequence. The old/new AEC comparison, capture handle field, and reopen call remain UNKNOWN.
