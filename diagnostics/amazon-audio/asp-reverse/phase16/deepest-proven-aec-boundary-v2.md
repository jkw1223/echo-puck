# Deepest proven AEC boundary, Phase 16

1. Production `dumpsys media.audio_flinger` independently reaches the vendor HIDL `Device::debug` path and crashes it.
2. With the overlay and no dumpsys, the app can query the registered standard AEC descriptor.
3. In this clean run, `AudioRecord` initialized with a valid session, but `AcousticEchoCanceler.create(session)` returned null.
4. The HIDL service remained alive for at least 15 seconds.

The deepest Phase 16 AEC point is therefore descriptor discovery plus AudioRecord creation. AEC instantiation was not successful in this run, and no enable, HAL effect attachment, CheckNativeEffect, start_echo_reference, or recording evidence exists.
