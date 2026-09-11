# Force-AEC state

`AudioSpeechEnhanceInfo::SetForceAECRec(bool)` is at `0x7d958` and `GetForceAECRecState()` at `0x7d968`. The symbols prove the state exists, but the current disassembly pass did not recover every writer or a proven caller chain from Android mode/source state. The binary strings show `mForceAECRec` and `GetForceAECRecState`; they do not prove an externally writable parameter.

No direct setter was called.
