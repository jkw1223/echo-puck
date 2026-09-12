# Effect pointer lineage

The configured pointer consumed by `CheckNativeEffect` originates at `config+0xe8+index*4`. The capture client owns the configuration pointer at `this+0x20`, and the preprocess object receives the same/associated configuration through `this+0x5c`. The retained static data does not prove that the pointer is copied from `AudioALSAStreamIn::addAudioEffect` argument; the stream stores its own pointer arrays at `+0x118`/`+0x150`. Lineage from the framework callback into `config+0xe8` remains UNKNOWN.
