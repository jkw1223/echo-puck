# `audio_hw_device` table

Ghidra resolved the device callback table data at `0x000f44bc` and a related table at `0x000f44c8`.

The primary device entries are:

```text
+0x00  0x0008d6f8  AudioALSAHardware::setMode
+0x0c  0x0008d778  AudioALSAHardware::setParameters
+0x10  0x0008f45c  AudioALSAHardware::getParameters
+0x24  0x00090830  AudioALSAHardware::openInputStream
+0x28  0x0009086c  AudioALSAHardware::closeInputStream
+0x1c  0x000907fc  openOutputStream
+0x20  0x00090828  closeOutputStream
```

The table is backed by Ghidra-resolved data pointers, not only symbol names. `AudioALSAHardware::setMode` and `setParameters` therefore have proven Android HAL table reachability. The exact C `audio_hw_device` field names depend on the platform header layout, but the callback addresses are stable in the recovered table.
