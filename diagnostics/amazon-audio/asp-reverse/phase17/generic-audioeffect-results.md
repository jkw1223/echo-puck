# Generic AudioEffect results

The harness reflectively invoked the four-argument `AudioEffect(UUID type, UUID uuid, int priority, int session)` constructor with standard AEC type `7b491460-8d4d-11e0-bd61-0002a5d5c51b`, implementation UUID zero, priority 0, and session 41.

The constructor failed with:

```text
java.lang.RuntimeException: Cannot initialize effect engine for type: 7b491460-8d4d-11e0-bd61-0002a5d5c51b Error: -3
```

This exposes native initialization result `-3` through the generic path. The harness did not enable the generic object (none was returned).
