# `SetAudioCommand` XREFs

Ghidra confirms `AudioALSAHardware::SetAudioCommand(int,int)` at `0x00090fa4` and the direct call to `SetForceAECRec` at `0x000912e0`. The device table recovered in `audio-hw-device-vtable.md` does not contain `SetAudioCommand`; no Android-facing table entry points to it. External reachability through Binder, ioctl, engineering services, or another library remains **UNKNOWN**.
