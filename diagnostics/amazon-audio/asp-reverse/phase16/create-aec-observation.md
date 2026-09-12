# Create AEC observation

With the temporary registration overlay applied and the diagnostic app foreground, Query Effects logged:

```text
aec_available=true
implementation=bb392ec0-8d4d-11e0-a896-0002a5d5c51b
type=7b491460-8d4d-11e0-bd61-0002a5d5c51b
```

The no-dumpsys lifecycle then logged:

```text
event=015 STEP=create_record state=1 session=9 rate=16000 channels=1 format=2 buffer=6400
event=016 STEP=create_aec null=true
```

The HIDL audio-service PID remained `4004` before Create AEC and after the required 15-second observation interval. No enable attempt was made because no AEC object existed.
