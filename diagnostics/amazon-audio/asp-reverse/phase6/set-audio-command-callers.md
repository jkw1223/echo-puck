# `SetAudioCommand` callers

`AudioALSAHardware::SetAudioCommand(int,int)` is the direct caller of `SetForceAECRec(bool)` in the captured HAL. Its command strings include `SET_AECREC_TEST_ENABLE` and `GET_AECREC_TEST_ENABLE`. No caller from `set_parameters`, Binder, ioctl, or another Android-visible callback was proven. External reachability is **UNKNOWN**.
