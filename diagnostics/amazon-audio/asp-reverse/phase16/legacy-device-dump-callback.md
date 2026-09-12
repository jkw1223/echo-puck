# Legacy HAL device dump callback

The existing ARM/Ghidra table reconstruction identifies the primary `audio_hw_device` table at `0x000f44bc`. Using the legacy `audio_hw_device` layout, the relevant entries are:

```text
+0x00 set_device                  0x0008d6f8  AudioALSAHardware::setMode
+0x0c set_parameters              0x0008d778  AudioALSAHardware::setParameters
+0x24 open_input_stream           0x00090830  AudioALSAHardware::openInputStream
+0x44 dump                        0x000908ac  AudioALSAHardware::dump
```

The stored dump value is a valid code address, not NULL or uninitialized. The symbol table identifies `0x000908ac` as:

```text
android::AudioALSAHardware::dump(int, android::Vector<android::String16> const&)
```

The related `dumpState` implementation is at `0x00090874`. This is consistent with the HIDL `Device::debug` path reaching the legacy HAL dump callback and then failing through an indirect null call in the wrapper's debug implementation. The callback slot itself is valid; the exact null target is the HIDL wrapper's internal dispatch state/control path, as shown by the tombstone PC of zero.
