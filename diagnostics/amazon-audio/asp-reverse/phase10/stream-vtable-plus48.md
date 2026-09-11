# Stream vtable `+0x48`

The `AudioALSAStreamIn` vtable/data table is at `0x000f19a0`. The entry at `+0x48` is `0x00056aec`, symbolized as `AudioALSAStreamIn::set(...)`, not an effect-forwarding callback. The constructor at `0x566a4` assigns the vtable through the relocated table pointer and stores `AudioALSAStreamManager::getInstance()` at object `+0x04`. Relevant entries are `+0x3c=0x57b44 addAudioEffect`, `+0x40=0x57e38 removeAudioEffect`, `+0x44=0x579e4 getCapturePosition`, `+0x48=0x56aec set`, `+0x4c=0x581c0 open`. Confidence: PROVEN.
