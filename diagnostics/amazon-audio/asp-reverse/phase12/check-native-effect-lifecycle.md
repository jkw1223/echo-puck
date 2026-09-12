# Synchronization timing

The capture-client wrapper at `0x9d310` proves lazy synchronization: it checks the configuration dirty byte at `config+0xdc`, invokes `AudioPreProcess::CheckNativeEffect`, then clears the byte. This is a capture-data-client lifecycle event rather than the stream `+0x48` callback. The exact caller event around `0x9c240`/`0x9e0b4` is not fully symbolized, so whether it is first read, capture start, reconfigure, or another processing transition remains UNKNOWN.
