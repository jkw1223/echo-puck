# Service PID timeline

## Baseline production dumpsys

```text
before: 3872
after:  3936
```

One `dumpsys media.audio_flinger` on the restored production XML produced a new `Device::debug` null-call tombstone (`tombstone_26`, 22:28:09) and changed the HIDL audio-service PID.

## No-dumpsys AEC observation

```text
overlay/start: 4004
before Query/Create: 4004
after Create AEC + 15 seconds: 4004
```

No HIDL service restart was observed during the no-dumpsys run.
