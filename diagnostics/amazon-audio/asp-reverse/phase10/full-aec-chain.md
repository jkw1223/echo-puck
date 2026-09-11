# Phase 10 evidence-graded chain

```text
AcousticEchoCanceler.create(session)           UNKNOWN
EffectsFactory registration                    UNKNOWN
RecordThread -> stream callback                UNKNOWN
AudioALSAStreamIn::addAudioEffect               PROVEN
AEC UUID recognized                             PROVEN
stream vtable +0x48 -> AudioALSAStreamIn::set   PROVEN
+0x48 -> AudioPreProcess::addAudioEffect        DISPROVEN for this target
other preprocess setup callers                 PROVEN at 0x8b258/0x8b2e0
AudioPreProcess::addAudioEffect                 PROVEN independently
start_echo_reference                            PROVEN under AEC/null-reference gate
capture reopen                                  UNKNOWN
reference PCM -> WriteReferenceBuffer           PROVEN downstream
```

The direct stream-to-preprocess edge is not present in the recovered callback. The next useful target is the surrounding preprocess setup path at `0x8b258`/`0x8b2e0` and its owning object.
