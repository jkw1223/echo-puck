# Preprocess effect pointer flow

`CheckNativeEffect` loads the configured effect pointer from `config = *(this+0x5c)`, at `config + 0xe8 + index*4`. It passes the address of that stored interface pointer as the second argument to `addAudioEffect(this, effect_interface_s**)` and `removeAudioEffect`. The current list is stored at `this + 8 + index*0xc`; each record’s interface pointer is compared against the configured pointer. This is a proven configuration-array synchronization path, separate from the stream effect array.
