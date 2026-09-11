# Candidate experiment

No runtime activation experiment is authorized by the static evidence yet. The preferred candidate remains `AudioManager.MODE_IN_COMMUNICATION` combined with `VOICE_COMMUNICATION`, but only after ARM-aware disassembly proves that mode 3 reaches `EnableNormalModeVoIP`, `SetForceAECRec`, or echo-reference setup.

There is currently no proven vendor parameter, mixer toggle, Binder control, or safe direct ASP call. The next static step is to recover the `audio_hw_device` and stream function tables and trace `str_parms` calls and mode/source comparisons with an ARM-aware decompiler. Until that is complete, production behavior remains unchanged.
