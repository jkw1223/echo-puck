# Phase 15 report: identify the preserved crash

## Result

The preserved Phase 14 crash is not an AEC-enable crash. It is a null indirect call in the vendor HIDL audio service's `Device::debug` implementation, and it occurred before the harness requested AEC enablement. The prior interpretation of `AudioFlinger server died` as proof that `AcousticEchoCanceler.setEnabled(true)` crashed audioserver is disproven by the timestamped evidence.

## Answers to the requested questions

1. **Process:** `/vendor/bin/hw/android.hardware.audio.service` (the HIDL audio service process; UID 1041). It is not `/system/bin/audioserver`.
2. **PID:** 2874. Crashing thread TID 3271, `HwBinder:2874_3`.
3. **Signal:** `SIGSEGV` (11), `SEGV_MAPERR`.
4. **Fault address:** `0x0`.
5. **Crashing PC:** `0x00000000`.
6. **Owning module/function:** The first meaningful saved frame is `/system/vendor/lib/hw/android.hardware.audio@2.0-impl.so`, `Device::debug(...)+46`, module-relative `0x144ab`, Build ID `99e3312e60223c5bc360708d9a8becea`.
7. **Ordering:** The tombstone timestamp is 22:06:45.689. `AecDiag event=016 STEP=create_aec` is 22:06:42.898. `AecDiag event=017 STEP=enable_aec` is 22:06:58.698. The crash predates the enable request by about 13 seconds.
8. **AOSP preprocessing:** Not proven to execute before this crash.
9. **Amazon HAL `add_audio_effect`:** Unknown in this run; no runtime evidence in the tombstone proves it.
10. **`AudioPreProcess::CheckNativeEffect()`:** Not proven.
11. **`start_echo_reference()`:** Not proven.
12. **Crash layer:** HIDL audio service / vendor audio HAL wrapper (`android.hardware.audio@2.0-impl.so`), specifically `Device::debug`, outside AudioFlinger effect processing and outside the AEC/ASP path shown in the tombstone.
13. **Minimal/no-op effect:** Unassessed. The captured crash does not reach a valid AEC enable boundary, so it cannot justify a replacement effect.
14. **Safest Phase 16 experiment:** First obtain a clean run with crash collection active and eliminate the independent HIDL `Device::debug` crash trigger. Then reproduce the exact sequence once, with no intervening `dumpsys`/debug transaction, and classify the new tombstone before considering any control-plane effect.

## Timeline

```text
22:06:38.646  event 001 query_effects
22:06:40.760  event 015 create_record
22:06:42.898  event 016 create_aec (non-null, control=true)
22:06:45.689  fatal SIGSEGV in HIDL Device::debug
22:06:45.997  tombstone_21 written; AudioFlinger death notifications follow
22:06:46.103  AudioEffect/AudioSystem report binder death
22:06:58.698  event 017 enable_aec status=-7, control=false
```

The `-7` result is therefore a consequence of the already-dead service/binder state, consistent with the user's instruction to treat it as a consequence rather than the root cause.

## Runtime boundary

The deepest runtime-proven AEC point remains Android-side effect creation: the standard AEC descriptor was registered and `AcousticEchoCanceler.create(session)` returned a controlled effect. The chain does not reach a proven enable command, RecordThread preprocessing, `audio_stream_in.add_audio_effect`, Amazon `AudioPreProcess`, or echo-reference setup.

## Cleanup

No new reproduction was performed because the preserved tombstone was sufficient to identify the actual crash and showed that the original experiment ordering was invalid. The production overlay was absent at verification, `com.steve.aecdiag` was uninstalled, `com.steve.puckd` remained installed, and the original XML hash was restored.
