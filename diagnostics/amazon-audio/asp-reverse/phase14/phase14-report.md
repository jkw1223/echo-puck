# Phase 14 report

## Harness

A standalone Java Android diagnostic APK was built as `com.steve.aecdiag`, separate from production `com.steve.puckd`. It requests only `RECORD_AUDIO`, has no service, network, wake-word, playback, Realtime, HAL, ASP, or mixer code, and exposes independent controls for query, record creation, AEC creation, enable, start/stop, disable, release, and snapshot. Event logs use the `AecDiag` tag with monotonic event numbering and elapsed time.

The production package remained installed and was not modified. The harness was uninstalled after testing.

## Gate 1 and Gate 2A

The temporary bind-mounted registration overlay loaded successfully. AudioFlinger exposed the expected AEC implementation and type UUIDs. The harness query independently confirmed:

```text
AcousticEchoCanceler.isAvailable() = true
NoiseSuppressor.isAvailable() = false
AutomaticGainControl.isAvailable() = false
```

`AudioEffect.queryEffects()` returned the AEC descriptor:

```text
name=Acoustic Echo Canceler
implementor=The Android Open Source Project
type=7b491460-8d4d-11e0-bd61-0002a5d5c51b
implementation=bb392ec0-8d4d-11e0-a896-0002a5d5c51b
```

The harness created `AudioRecord` with `VOICE_RECOGNITION`, 16 kHz, mono, PCM16, buffer 6400, session ID 25, state initialized. It then created the AEC object without enabling it:

```text
null=false
enabled=false
control=true
```

This proves Gate 2A.

## Gate 2B stop condition

When the separate Enable AEC control was pressed, the harness logged:

```text
STEP=enable_aec status=-7 before=false after=false control=false
```

At the same time, logcat reported:

```text
AudioFlinger server died!
```

The AudioFlinger process restarted and normal system audio initialization messages followed. This is an explicit stop condition. Recording was not started, the after-start ordering was not attempted, and no leakage test was run.

The logs do not prove Amazon-side AEC activation. No reliable `AudioALSAStreamIn::addAudioEffect`, `CheckNativeEffect`, `start_echo_reference`, reverse-configuration, or capture-reopen event was observed before the server death. The result is therefore:

```text
framework registration: PROVEN
AudioRecord creation: PROVEN
AEC object creation while disabled: PROVEN
AEC enablement: FAILED / STOPPED on AudioFlinger death
Amazon internal AEC activation: UNPROVEN
```

## Rollback

The bind mount was removed, the staged XML deleted, audioserver restarted, and the original XML hash restored:

```text
61c6fe7cdd977c32b0014bdf1b07277b1cb7bebe1369b8907660fdee72d639a3
```

The temporary harness was uninstalled. Final package inventory contains `com.steve.puckd` and no `com.steve.aecdiag`. AudioFlinger no longer reports `pre_processing`.

## Answers

1. Standalone harness installed without changing PuckD: **yes**.
2. `AudioEffect.queryEffects()` saw AEC: **yes**.
3. `isAvailable()` returned true: **yes**.
4. `AudioRecord` initialized: **yes; session 25**.
5. `AcousticEchoCanceler.create()` succeeded: **yes**.
6. New AEC initially disabled: **yes**.
7. Creation alone attached effect: **not shown to alter Amazon HAL state**.
8. Creation alone produced Amazon-side changes: **none proven**.
9. Enable changed state: **setEnabled returned -7, control was lost, AudioFlinger died**.
10. Recording-start change: **not tested**.
11. Ordering comparison: **not completed**.
12. Capture reopening: **not observed**.
13. `start_echo_reference()`: **not observed**.
14. Reverse processing: **not observed**.
15. PCM health with AEC attached: **not tested after enable**.
16. Release reversal: **not validly observed**.
17. Amazon AEC path runtime-proven: **no**.
18. Phase 15 leakage testing justified: **no**.
