# Full AEC chain v4

```text
framework AEC registration                         PROVEN ABSENT in current XML
AcousticEchoCanceler.isAvailable() == true         DISPROVEN; observed false
AudioFlinger loads pre_processing                   DISPROVEN; dump lists no such library
AudioALSAStreamIn::addAudioEffect                  PROVEN internally
stream AEC +0x114 write                             PROVEN
configuration dirty/effect array                   PROVEN at config+0xdc/+0xe0/+0xe8
capture-client CheckNativeEffect                   PROVEN at 0x9d310
AudioPreProcess::CheckNativeEffect                 PROVEN at 0x8b050
AudioPreProcess::addAudioEffect                    PROVEN
AEC UUID recognition                                PROVEN
start_echo_reference                               PROVEN under gate
capture reopen                                     UNKNOWN
reference PCM -> SPELayer::WriteReferenceBuffer    PROVEN downstream
```
