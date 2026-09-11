# Audio mode routing

`AudioALSAHardware::setMode(int)` at `0x8d6f8` forwards into `AudioALSAStreamManager::setMode(audio_mode_t)` at `0x5b6a4`. The manager function is substantial and contains mode/state branches, but this pass did not recover a reliable symbolic mapping proving that mode 3 (`AUDIO_MODE_IN_COMMUNICATION`) reaches `EnableNormalModeVoIP`, `SetForceAECRec`, or echo-reference creation.

Consequently, `MODE_IN_COMMUNICATION` remains an untested hypothesis and must not yet be run as an activation experiment.
