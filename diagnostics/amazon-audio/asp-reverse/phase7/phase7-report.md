# Phase 7 report

## What this phase established

Ghidra 12.1.3 was installed self-contained and used to import the exact 32-bit ARM HAL. Auto-analysis completed. Ghidra recovered the Android-facing device and input-stream callback tables.

Proven table path:

```text
audio_hw_device table 0x000f44bc
  -> set_mode         0x0008d6f8 AudioALSAHardware::setMode
  -> set_parameters   0x0008d778 AudioALSAHardware::setParameters
  -> open_input_stream 0x00090830 AudioALSAHardware::openInputStream

audio_stream_in table 0x000f19a0
  -> set_parameters   0x000575e0 AudioALSAStreamIn::setParameters
  -> add_audio_effect 0x00057b44 AudioALSAStreamIn::addAudioEffect
  -> remove_effect    0x00057e38 AudioALSAStreamIn::removeAudioEffect
  -> read             0x00057094 AudioALSAStreamIn::read
```

This closes the Android-visible callback-table edge. The direct call from the input callback to `AudioPreProcess::addAudioEffect` is not yet decompiler-proven because the native decompiler component is missing from the Intel distribution. The exact effect UUID and the original echo-reference PCM producer are also unresolved.

## Completion criterion

Phase 7 does **not** yet meet the requested success criterion. The current chain is:

```text
Android audio_stream_in.add_audio_effect
  -> AudioALSAStreamIn::addAudioEffect                 PROVEN
  -> AudioPreProcess::addAudioEffect                   UNKNOWN
  -> descriptor match                                  UNKNOWN
  -> start_echo_reference                              PROVEN as a callable target
```

No runtime activation was performed. The production APK and vendor partition remain unchanged.

## Single remaining blocker

The single central unresolved edge is native ARM data-flow inside `AudioALSAStreamIn::addAudioEffect`: resolving the descriptor structure/UUID comparison and proving its call to `AudioPreProcess::addAudioEffect`. The Intel host's Ghidra distribution can analyze ARM instructions and tables but cannot launch its native decompiler component.

The safest next step is to run the preserved Ghidra project on a supported host with the native ARM decompiler component, then export the pseudocode and UUID bytes. Do not run the AEC experiment before that edge is resolved.
