# `CheckNativeEffect` callers

The direct call XREFs are:

```text
0x0009d364  AudioALSACaptureDataClient::CheckNativeEffect()  -> AudioPreProcess::CheckNativeEffect(this + 0x1b54)
0x0009c240  call-site reference in the capture-client processing region (raw disassembly XREF; enclosing symbol not recovered by the current function database)
0x0009e0b4  second PLT call-site reference in the capture processing region
```

The fully decompiled caller is `AudioALSACaptureDataClient::CheckNativeEffect` at `0x9d310`. It gates on `*(this+0x20)+0xdc != 0`, calls the preprocess synchronizer through `this+0x1b54`, then clears the configuration dirty byte at `config+0xdc`. This identifies the lifecycle owner of synchronization as the capture data client.
