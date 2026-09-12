# Effect descriptor analysis

The registered descriptor is:

```text
name: Acoustic Echo Canceler
implementor: The Android Open Source Project
type: 7b491460-8d4d-11e0-bd61-0002a5d5c51b
implementation: bb392ec0-8d4d-11e0-a896-0002a5d5c51b
```

`AudioEffect.queryEffects()` exposes the descriptor. The Phase 17 static creation returned a controlled, initially disabled object. No enable command or effect flags beyond the public descriptor were queried, so connection-mode/offload flag claims remain unassessed.
