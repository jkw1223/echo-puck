# Deepest proven AEC boundary

PROVEN: Android application creates an `AcousticEchoCanceler` for the `AudioRecord` session and receives a non-null effect with control (`AecDiag event=016`).

NOT PROVEN: `setEnabled(true)` entering the AOSP software effect, AudioFlinger EffectModule, RecordThread preprocessing, `audio_stream_in.add_audio_effect`, Amazon `AudioALSAStreamIn::addAudioEffect`, `CheckNativeEffect`, `start_echo_reference`, or ASP reverse processing.

The only native crash captured in the preserved run is `/vendor/bin/hw/android.hardware.audio.service` in HIDL `Device::debug`, with PC 0, and it occurs before the harness's enable event. It is therefore not evidence of progress into the AEC activation chain.
