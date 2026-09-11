# `audio_stream_in` table

Ghidra resolved the `AudioALSAStreamIn` C++ vtable/data table at `0x000f19a0`. The recovered entries are:

```text
+0x08  0x00056758  AudioALSAStreamIn::~AudioALSAStreamIn
+0x0c  0x000567f8  deleting destructor
+0x10  0x00056fcc  sampleRate
+0x14  0x00056fd4  bufferSize
+0x18  0x00056fdc  channels
+0x1c  0x00056fe4  format
+0x20  0x00056fec  setGain
+0x24  0x00057094  read
+0x28  0x00053248  getInputFramesLost / dump-shared implementation
+0x2c  0x00057450  standby
+0x30  0x000575e0  setParameters
+0x34  0x0005794c  getParameters
+0x38  0x00053248  dump / getInputFramesLost shared implementation
+0x3c  0x00057b44  addAudioEffect
+0x40  0x00057e38  removeAudioEffect
+0x44  0x000579e4  getCapturePosition
+0x48  0x00056aec  set/routing helper
+0x4c  0x000581c0  open
```

The table provides a proven Android-facing callback-to-implementation mapping for `addAudioEffect`, `removeAudioEffect`, `setParameters`, and `read`. Ghidra XREFs show the data table reference at `0x000f38ec` for `addAudioEffect` and `0x000f38e0` for `setParameters`.

The remaining edge is inside `AudioALSAStreamIn::addAudioEffect` (`0x57b44`): the native decompiler component is unavailable on this Intel Ghidra distribution, so the descriptor comparison and call into `AudioPreProcess::addAudioEffect` remain to be reconstructed from ARM code.
