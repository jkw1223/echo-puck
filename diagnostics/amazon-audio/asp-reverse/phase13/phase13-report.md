# Phase 13 report

## Gate 0

The production XML and preprocessing library were hashed and preserved before modification. Original XML SHA-256:

```text
61c6fe7cdd977c32b0014bdf1b07277b1cb7bebe1369b8907660fdee72d639a3
```

The preprocessing library SHA-256 was preserved in `registration-before.txt`. The Show was reachable at `192.168.1.24:5555`; adb root/remount succeeded; SELinux was permissive; original ownership/mode/context were recorded.

## Gate 1

Only the following were added to a test copy of the XML:

```xml
<library name="pre_processing" path="libaudiopreprocessing.so"/>
<effect name="aec" library="pre_processing" uuid="bb392ec0-8d4d-11e0-a896-0002a5d5c51b"/>
```

The XML parsed locally. After installation and audioserver restart, AudioFlinger reported:

```text
Library pre_processing
path: /vendor/lib/soundfx/libaudiopreprocessing.so
Acoustic Echo Canceler / The Android Open Source Project
UUID: bb392ec0-8d4d-11e0-a896-0002a5d5c51b
TYPE: 7b491460-8d4d-11e0-bd61-0002a5d5c51b
```

Gate 1 therefore succeeded: the implementation loaded and the expected descriptor was visible to AudioFlinger. `AudioEffect.queryEffects()` was not called directly because the installed diagnostic app couples that query to its capture-start path; the AudioFlinger descriptor dump is the preserved registration evidence.

## Gate 2

Gate 2 was stopped before effect creation. The diagnostic app’s AudioRecord failed to initialize after the audioserver restart:

```text
AudioFlinger could not create record track, status: -1
Error code -20 when initializing native AudioRecord object
```

No `AcousticEchoCanceler.create(sessionId)` call was made. No effect was enabled. No Amazon AEC activation, CheckNativeEffect transition, capture reopen, or echo-reference creation was tested.

## Restoration

The original XML was restored, mode/owner/context reset, and audioserver restarted. The restored hash matches the Gate 0 hash. The restored AudioFlinger state reports no `pre_processing` library. No production APK or HAL binary was modified.

## Answers

1. `libaudiopreprocessing.so` loaded after registration: **yes**.
2. Expected AEC descriptor became visible to AudioFlinger: **yes**.
3. `AcousticEchoCanceler.isAvailable()` was directly queried after registration: **not collected**; no effect object was created.
4. Reported type UUID: `7b491460-8d4d-11e0-bd61-0002a5d5c51b`.
5. Reported implementation UUID: `bb392ec0-8d4d-11e0-a896-0002a5d5c51b`.
6. `AcousticEchoCanceler.create(sessionId)` succeeded: **not attempted**.
7. Amazon HAL AEC activation: **not tested**.
8. Capture reopening: **not observed**.
9. Echo reference creation: **not observed at runtime**.
10–12. Leakage and speech preservation: **not measured**.
13. Effect removal reversal: **not tested** because no effect was created.
14. Original ROM/application state: **restored and hash-verified**.
