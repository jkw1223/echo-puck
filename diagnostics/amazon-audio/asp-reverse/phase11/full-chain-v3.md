# Full chain v3

```text
Android registered AEC descriptor                 UNKNOWN
  -> AcousticEchoCanceler.isAvailable()           UNKNOWN/observed false
  -> AudioFlinger attaches effect                 UNKNOWN
  -> AudioALSAStreamIn::addAudioEffect             PROVEN
  -> stream +0x114 AEC flag                        PROVEN write
  -> AudioPreProcess::CheckNativeEffect            PROVEN function 0x8b050
  -> configured effect pointer config+0xe8         PROVEN
  -> AudioPreProcess::addAudioEffect               PROVEN calls 0x8b258/0x8b2e0
  -> AEC UUID recognized                            PROVEN
  -> start_echo_reference                           PROVEN under gate
  -> capture reopen                                UNKNOWN
  -> reference PCM -> WriteReferenceBuffer          PROVEN downstream
```

The synchronization mechanism is now proven inside the preprocess object. The event that invokes `CheckNativeEffect` and the framework-to-stream registration edge remain unresolved.
