# Crash symbolication

## Exact tombstone

Source: `tombstone_21`, timestamp `2026-09-11 22:06:45-0500`.

- Process: `/vendor/bin/hw/android.hardware.audio.service`
- PID: `2874`
- Crashing thread: `HwBinder:2874_3`, TID `3271`
- UID: `1041` (`audioserver`)
- Signal: `SIGSEGV` (11), `SEGV_MAPERR`
- Fault address: `0x0`
- Register PC: `0x00000000`
- Cause: null pointer dereference

## Native stack

The first non-null frame is:

```text
#01 pc 000144ab /system/vendor/lib/hw/android.hardware.audio@2.0-impl.so
    android::hardware::audio::V2_0::implementation::Device::debug(...)+46
    Build ID 99e3312e60223c5bc360708d9a8becea
```

The exact device binary was pulled and hashed in `crashing-binary-hashes.txt`. It is an ARM 32-bit stripped ELF. The crash PC itself is zero, so there is no module-relative instruction at the fault PC. The tombstone's saved link/stack and frame #1 establish an indirect null control transfer from `Device::debug`; the meaningful owning module is `android.hardware.audio@2.0-impl.so`, at function offset `0x144ab` (`Device::debug + 46`).

Remaining frames are generic HIDL dispatch (`libhidlbase.so`, `android.hardware.audio@2.0.so`) and thread-pool code. No `libaudiopreprocessing.so`, `libaudioflinger.so`, Amazon primary HAL, ASP, or `AudioPreProcess` frame appears.
