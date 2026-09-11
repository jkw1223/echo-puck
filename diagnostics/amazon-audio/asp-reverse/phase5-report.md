# Phase 5 narrowly scoped control-flow reconstruction

## Boundary

No vendor library was patched, loaded into the device, or executed. No runtime activation was attempted. The available host tools did not include Ghidra/radare2; the reconstruction uses the ARM ELF symbols, ARM `objdump` disassembly, PLT call sites, relocation-visible names, and string/symbol address evidence already collected in `asp-reverse/`.

## Confirmed entry points

The primary HAL is a 32-bit ARM EABI library. Confirmed exported internal functions include:

```text
AudioALSAHardware::setMode(int)                         0x8d6f8
AudioALSAHardware::setParameters(String8 const&)        0x8d778
AudioALSAHardware::SetAudioCommand(int,int)             0x90fa4
AudioALSAStreamManager::setMode(audio_mode_t)            0x5b6a4
AudioSpeechEnhanceInfo::SetForceAECRec(bool)             0x7d958
AudioSpeechEnhanceInfo::GetForceAECRecState()            0x7d968
SPELayer::EnableNormalModeVoIP(bool)                    0x85e50
SPELayer::WriteReferenceBuffer(InBufferInfo*)            0x84a90
SPELayer::Process_VoIP(short*,int)                      0x89240
AudioPreProcess::start_echo_reference(audio_format_t,..) 0x8ba50
AudioPreProcess::push_echo_reference(unsigned int)       0x8c23c
```

The exported C++ methods are not themselves the complete Android function-table recovery. The stripped binary's `audio_hw_device` and `audio_stream_in` table assignments still require an ARM-aware decompiler or manual relocation/table reconstruction.

## Device parameter path

`AudioALSAHardware::setParameters(String8 const&)` is confirmed at `0x8d778`. It constructs `AudioParameter` and calls `getFloat`, `getInt`, and `remove`. The exported `AudioParameter` key objects present in the HAL are:

```text
keyRouting
keySamplingRate
keyFormat
keyChannels
keyFrameCount
keyInputSource
keyScreenState
```

The recovered function visibly consumes the standard master-volume float and integer stream/device controls, then routes values into stream-manager/speech-driver methods. This is not evidence of an AEC vendor key. No exact external key that writes `mForceAECRec` or `AECOn` was confirmed.

## `SetForceAECRec` caller

There is one direct relocation-visible caller in the HAL:

```text
AudioALSAHardware::SetAudioCommand(int par1, int par2) 0x90fa4
    -> AudioSpeechEnhanceInfo::SetForceAECRec(bool)      0x7d958
```

The caller converts `par2` to a boolean (`cmp r4,#0; movne r1,#1; moveq r1,#0`) immediately before the call. The surrounding command dispatcher compares `par1` against a set of numeric audio-test commands; the string table labels the relevant command branch `SET_AECREC_TEST_ENABLE(%d)` / `GET_AECREC_TEST_ENABLE=%d`. This establishes an internal/test command path, not an Android-visible `set_parameters` key. The command numeric identity is not promoted without decoding the vendor enum table.

## `start_echo_reference` callers

The direct call sites are:

```text
AudioPreProcess::addAudioEffect(effect_interface_s**) 0x8b?  -> start_echo_reference@plt
AudioPreProcess::start_echo_reference(...)              0x8ba50
AudioPreProcess::WriteEchoRefData(...)                  internal reference write path
```

The `addAudioEffect` slice contains a `memcmp` against an effect descriptor and then calls `start_echo_reference` with format/channel/rate fields loaded from the effect/input object. This is the first concrete evidence tying echo-reference creation to an input effect/preprocessing lifecycle rather than to source 7 alone. The exact UUID bytes require decoding the descriptor object in a decompiler.

## `WriteReferenceBuffer` callers

Two direct call sites are visible from `AudioALSACaptureDataClient::copyEchoRefCaptureDataToClient(RingBuf&)` around `0x9fc60` and `0x9fca4`. The caller loads ring-buffer/client fields and passes an `InBufferInfo*`. This confirms a capture-data/ring-buffer bridge into `SPELayer::WriteReferenceBuffer`, but does not by itself prove that the original PCM is the speaker `out_write` buffer.

## AEC reopen branch

The exact log string `AECOn, need reopen the capture handle` is present in the capture-open/configuration code, alongside `input_source`, `mAudioMode`, input device, and `mBypassDualMICProcessUL`. The ARM disassembly confirms this is a state-transition diagnostic, but the stripped build does not provide enough high-level structure recovery to name the old/new state variables or prove the complete close/reopen sequence. The branch is preserved in the disassembly slices under `phase4/`.

## Mode/source comparisons

`AudioALSAHardware::setMode(int)` logs the mode and forwards to `AudioALSAStreamManager::setMode(audio_mode_t)`. The manager has extensive mode branches. This pass did not recover a proven comparison against Android mode 3 that reaches `EnableNormalModeVoIP`, `SetForceAECRec`, or `start_echo_reference`.

The HAL visibly logs `input_source`, `bVoIPEnable`, and mode together in capture configuration. It does not expose a relocation-visible direct branch proving that source 7 alone activates the Amazon AFE. This is consistent with the controlled VR/VC result.

## Effect descriptors

`AudioPreProcess::addAudioEffect` contains a descriptor comparison before the `start_echo_reference` call. The exact effect UUID/descriptor value was not recovered from the available symbol/relocation output, so no UUID is asserted here. Android runtime effect enumeration previously showed no exposed AEC/NS/AGC effect.

## Current activation conclusion

The evidence does not yet form the requested continuous chain from an Android-visible operation to `start_echo_reference()` or `SetForceAECRec(true)`. The strongest proven chain is:

```text
internal AudioALSAHardware::SetAudioCommand(par1,par2)
  -> SET_AECREC_TEST_ENABLE command branch
    -> SetForceAECRec(par2 != 0)
```

That is an internal/test command path, not a proven Android framework operation. A second partial chain is:

```text
AudioPreProcess::addAudioEffect(effect descriptor match)
  -> start_echo_reference(format, channels, rate)
```

The missing link is the exact Android-visible effect or HAL table operation that reaches `addAudioEffect`, and whether that effect is installed by the normal capture path.

## Safe next static step

Load the ARM binary in Ghidra or an equivalent decompiler and recover the `audio_hw_device` / `audio_stream_in` function-pointer tables, the exact `AudioParameter` key literals at each `get*` call, and the effect descriptor UUID bytes in `addAudioEffect`. Until those three links are recovered, do not set mode 3, send vendor parameters, invoke `SetAudioCommand`, or activate any proprietary path.
