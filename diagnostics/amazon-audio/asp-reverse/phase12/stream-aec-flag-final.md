# Stream AEC flag

Proven writes:

```text
0x57b44 AudioALSAStreamIn::addAudioEffect     this+0x114 = 1 on AEC UUID
0x57e38 AudioALSAStreamIn::removeAudioEffect  this+0x114 = 0 on AEC UUID
```

A complete read XREF set for `this+0x114` was not recovered. The capture-client synchronizer uses its own configuration dirty/effect fields, not a visible direct load from the stream object. The translation from stream `+0x114` to `config+0xdc/+0xe8` remains UNKNOWN.
