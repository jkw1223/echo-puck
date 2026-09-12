# Preprocess configuration owner

`AudioPreProcess::CheckNativeEffect` loads its configuration pointer from `AudioPreProcess + 0x5c`. The capture client has a parallel configuration pointer at `AudioALSACaptureDataClient + 0x20`; its `CheckNativeEffect` reads `config+0xdc`, `config+0xe0`, and invokes the preprocess object at `+0x1b54`. The precise C++ type name of the configuration object is not recovered, but its role is proven: dirty flag at `+0xdc`, effect count at `+0xe0`, configured effect-interface pointers beginning at `+0xe8`.
