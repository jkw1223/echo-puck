# Phase 17 report

## Result

Phase 17 exposed the hidden generic initialization result and recovered a successful static AEC creation without enabling it.

The public static helper produced:

```text
session=41
AcousticEchoCanceler.create(session) -> non-null
enabled=false
hasControl=true
type=7b491460-8d4d-11e0-bd61-0002a5d5c51b
implementation=bb392ec0-8d4d-11e0-a896-0002a5d5c51b
```

The direct constructor path was not available through reflection (`NoSuchMethodException`). The generic `AudioEffect` constructor exposed:

```text
RuntimeException
Cannot initialize effect engine for type: 7b491460-8d4d-11e0-bd61-0002a5d5c51b Error: -3
```

The service PID remained `4405` throughout. No enable, speaker test, proprietary API, or dumpsys call was made.

## Required answers

1. Hidden generic native status: `-3`, surfaced as `RuntimeException` by the generic constructor.
2. Direct construction: unavailable; no reflective `<init>(int)` exists.
3. Generic construction: clearer error, native `-3`.
4. Effect library instantiation: the public AEC helper's non-null object proves its implementation path instantiated; the generic type-only request failed before returning an object.
5. Recording session: valid, initialized state 1, active recording state 1, session 41, 16 kHz mono PCM16, source 6 (`VOICE_RECOGNITION`).
6. Active recording prerequisite: not established; the successful AEC creation occurred with the AudioRecord already in recording state due to the prior tap sequence. No controlled Order B comparison was run.
7. Query preprocessings: unavailable; method absent from this framework class.
8. Phase 14 vs Phase 16: creation is state/resource dependent or intermittent. Phase 14 and Phase 17 succeeded; Phase 16 returned null. The Phase 14 service death was independently caused by dumpsys, not AEC creation.
9. Determinism: not deterministic failure across the preserved sessions.
10. Exact missing prerequisite: unknown. Generic type-only creation returns `-3`, while the helper can succeed; no safe evidence identifies the prerequisite.
11. Reproducible non-null sequence: one successful Phase 17 Order A exists, but it is not yet proven repeatable.
12. Clean enablement: not justified until creation repeatability and the generic/helper discrepancy are understood. AEC remained disabled throughout.

## Cleanup

The overlay was removed, the staged XML deleted, the diagnostic APK uninstalled, and the production XML hash was restored. `com.steve.puckd` remained installed. Verification used hash/package queries only; no `dumpsys media.audio_flinger` was used.
