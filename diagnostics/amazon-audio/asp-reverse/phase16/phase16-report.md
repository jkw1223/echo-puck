# Phase 16 report

## Outcome

Phase 16 proved that `dumpsys media.audio_flinger` independently crashes the vendor HIDL audio service. A single baseline invocation on the normal production XML changed the service PID from `3872` to `3936` and created `tombstone_26` with the same null `Device::debug` crash seen in Phase 14. The command is banned for the remainder of the AEC investigation.

A clean no-dumpsys AEC observation then showed a stable HIDL service PID (`4004`) for at least 15 seconds after AudioRecord creation. Query Effects found the registered standard AEC descriptor, and AudioRecord initialized, but `AcousticEchoCanceler.create()` returned null. Consequently no enable, recording, or removal lifecycle was attempted.

## Required answers

1. **Legacy dump callback:** table `0x000f44bc + 0x44` stores valid address `0x000908ac`, `AudioALSAHardware::dump`; it is not NULL.
2. **Independent dumpsys crash:** yes. One production invocation produced the matching `Device::debug` SIGSEGV and changed the service PID.
3. **Phase 14 timing:** yes. Phase 14's crash occurred before the later enable log; Phase 16 reproduced it directly with dumpsys.
4. **AEC creation survival:** the HIDL service survived at least 15 seconds in the no-dumpsys run.
5. **PID stability:** yes, `4004` remained stable during the observation interval.
6. **Clean enable return:** not applicable; `create()` returned null, so enable was not attempted.
7. **Enable crash:** not tested; no valid effect existed.
8. **AEC enabled:** no.
9. **Recording with AEC:** not tested.
10. **PCM health:** not applicable.
11. **Amazon-side evidence:** no AEC attachment, `CheckNativeEffect`, `start_echo_reference`, or reverse-processing evidence. The log contains normal ASP speaker pipeline initialization only.
12. **Removal:** no AEC object existed; overlay and package cleanup completed.
13. **Phase 14 `-7`:** the Phase 14 result is explained as a consequence of the already-dead binder service. Phase 16 proves dumpsys is an independent cause of that service death; it does not prove a clean enable would succeed.
14. **Phase 17 leakage:** not justified. AEC instantiation did not succeed in the clean no-dumpsys run, and no valid enable/recording path exists yet.

## Cleanup

The overlay was unmounted, staged XML removed, diagnostic APK uninstalled, and the original XML hash restored:

```text
61c6fe7cdd977c32b0014bdf1b07277b1cb7bebe1369b8907660fdee72d639a3
```

`com.steve.puckd` remained installed. One post-cleanup verification accidentally invoked the now-banned `dumpsys`; it produced `tombstone_27`, which is preserved separately and is another instance of the same known diagnostic crash. No further dumpsys calls were made.
