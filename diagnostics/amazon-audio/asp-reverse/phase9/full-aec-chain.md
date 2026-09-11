# Full chain with evidence grades

```text
AcousticEchoCanceler.create(session)                 UNKNOWN
  -> AudioEffect/EffectsFactory registration          UNKNOWN
  -> RecordThread calls audio_stream_in callback      UNKNOWN
  -> AudioALSAStreamIn::addAudioEffect               PROVEN (0x57b44)
  -> direct/indirect AudioPreProcess edge              UNKNOWN
  -> AudioPreProcess::addAudioEffect                 PROVEN as an independent function (0x8b734)
  -> standard AEC UUID recognized                     PROVEN
  -> preprocess AEC flag set                          PROVEN
  -> start_echo_reference                             PROVEN when +0x48 is null
  -> create_echo_reference                            PROVEN
  -> in_configure_reverse                             PROVEN
  -> capture reopen                                   UNKNOWN
  -> reference PCM into WriteEchoRefData              PROVEN for downstream producer
  -> SPELayer::WriteReferenceBuffer                    PROVEN
```

The chain is not continuous enough to justify a runtime experiment.
