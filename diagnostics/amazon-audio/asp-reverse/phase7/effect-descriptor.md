# Effect descriptor / AEC path

Ghidra identified `AudioALSAStreamIn::addAudioEffect` at `0x00057b44` and `AudioPreProcess::addAudioEffect` at `0x0008b734` (with the PLT entry at `0x00037db4`). The input-stream table points to the former, proving the Android-facing callback implementation.

The remaining descriptor branch is unresolved. Ghidra auto-analysis found the functions and data references, but its Intel macOS distribution lacks the native decompiler component (`os/mac_x86_64/decompile`). Without decompiler data-flow, the exact `effect_descriptor_t` layout, 16-byte UUID bytes, and standard-versus-vendor classification cannot be stated as PROVEN.

Status: callback edge **PROVEN**; descriptor/UUID edge **UNKNOWN**.
