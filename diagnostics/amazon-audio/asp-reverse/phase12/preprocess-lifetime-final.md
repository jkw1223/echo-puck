# Preprocess lifetime

`AudioPreProcess` constructor: `0x8ac14`; destructor begins `0x8adcc`. `AudioALSACaptureDataClient` stores an `AudioPreProcess*` at `this+0x1b54` and invokes its `CheckNativeEffect`. This proves capture-client ownership/reference at runtime. Whether the object is one-per-client or shared across clients is not established by the current evidence.
