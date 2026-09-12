# Order A

```text
Query Effects
Create AudioRecord
Query Preprocessings
Create AEC
Create Generic AudioEffect
```

Observed:

```text
AudioRecord state=1 recording_state=1 session=41 rate=16000 channels=1 format=2
queryPreProcessings: NoSuchMethodException
AcousticEchoCanceler.create: non-null, enabled=false, hasControl=true
Generic AudioEffect: RuntimeException, native Error -3
```

No effect was enabled and no recording was started.
