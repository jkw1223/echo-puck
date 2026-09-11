# Preprocess effect callers

The disassembly contains direct calls to the `AudioPreProcess::addAudioEffect` PLT at `0x8b258` and `0x8b2e0`, both inside the constructor/initialization-region functions immediately preceding `0x8b360` (`removeAudioEffect`) and `0x8b734` (`addAudioEffect`). Direct calls to `AudioPreProcess::removeAudioEffect` occur at `0x8b0dc` and `0x8b160`. No call from `AudioALSAStreamIn::addAudioEffect` or its `+0x48` target was found. This establishes that preprocess add/remove are reached by another preprocess-owned setup path, not by the recovered stream callback.
