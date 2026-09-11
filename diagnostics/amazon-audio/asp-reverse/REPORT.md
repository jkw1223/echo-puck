# ASP/AFE static reverse-engineering report

## Evidence boundary

This phase is static only. No proprietary function was called, no vendor binary was patched, and no mixer control was changed. The libraries are copies of the device files collected from the Show. Metadata, symbol, string, dependency, and disassembly outputs are adjacent to this report.

## Dependency graph

```text
audio.primary.amazon_wrapper.so
  -> hardware/logging/C++ system libraries

audio.primary_amazon.mt8163.so (SONAME audio.primary.mt8163.so)
  -> libasp.so
  -> libaudiosetting.so
  -> libaudiocomponentengine.so
  -> libaudiotoolkit.so
  -> libtinyalsa.so / libtinycompress.so
  -> MediaTek speech, NVRAM, Bluetooth, and utility libraries

libasp.so
  -> Android media/binder/log/utils/cutils/audioutils and system libraries

libaspclient.so
  -> Android binder/log/utils and system libraries
  -> exposes com.amazon.asp.IAudioSignalProcessor Binder interfaces
```

The HAL directly imports `libasp.so`; the static NEEDED list does not show a direct HAL dependency on `libaspclient.so`. `libaspclient.so` is a separate Binder client interface, not the immediate HAL-to-ASP dependency demonstrated by this binary set.

## Exported ASP API

`libasp.so` exports a small C API:

```text
asp_init
asp_create_pipeline
asp_destroy_pipeline
asp_command
asp_process
asp_process_ext
asp_set_device
asp_set_event_callback
asp_set_ext_ref_sync_status
```

The HAL strings include `asp_open`, `asp_close`, `asp_init()`, `asp_set_device()`, and `asp_process`, establishing that the normal audio HAL path initializes and processes through this API. Exact argument types require ABI-aware disassembly; this phase did not execute or infer a safe call ABI.

`libaspclient.so` is an Android Binder interface library. Its exported C++ classes include `IAudioEventListener`, `BnAudioEventListener`, `IAudioSignalProcessor`, and `BpAudioSignalProcessor`, with descriptors `com.amazon.asp.IAudioEventListener` and `com.amazon.asp.IAudioSignalProcessor`. It does not expose the same C `asp_*` processing API.

## HAL and parameter evidence

The HAL contains strings for `input_source`, `routing`, `setMode`, `AECOn, need reopen the capture handle`, `mForceAECRec`, `GetForceAECRecState`, `SetForceAECRec`, `SetDynamicVoIPSpeechEnhancementMask`, `EnableNormalModeVoIP`, and `WriteReferenceBuffer`. It also contains `asp_command` and a speaker-power notification path.

The strongest static finding is that AEC state is tied to capture-handle lifecycle: changing `AECOn` can require reopening the capture handle. The binary contains no clear public key/value string such as `aec_enable=true` or `asp_enable=true`; the accepted-parameter table therefore remains unresolved from strings alone. `set_parameters`/`get_parameters` implementations should be recovered with a full ARM-aware decompiler before proposing a runtime key.

## Echo-reference path

The HAL imports/contains `create_echo_reference`, `release_echo_reference`, `start_echo_reference`, `stop_echo_reference`, `clear_echo_reference`, `remove_echo_reference`, `add_echo_reference`, `update_echo_reference`, `push_echo_reference`, `get_capture_delay`, and `get_echoref_delay`. It also contains `WriteReferenceBuffer` and `mDLInBufferQ`/downlink queue strings.

This indicates an internal AudioPreProcess echo-reference object fed by playback/downlink data, rather than an Android `AudioEffect` chain. The strings explicitly validate format and channel counts and refer to reference frame lengths and playback/VOIP buffers. The exact source PCM device or AudioFlinger handoff is not proven by strings alone.

## AFE activation path

`libasp.so` contains configuration and runtime strings for `Enable AEC`, `Automatic AEC/ABF Selection according to reference power`, `setInVOIPMode : %d`, `Alarm : set AEC adaptation %d`, `BeamSelector`, `RefBeamSelector`, `SIRBeamSelector`, and `AlignedBeamMerger`. It also contains multi-mic VOIP setup, reference downsampler, gain ramp, and reference-channel selection diagnostics.

The HAL contains `asp_init`, `asp_set_device`, and `asp_process` call paths plus `asp_open`/`asp_close`. The retained `AFE.cfg` concepts therefore appear to be consumed by ASP pipeline initialization/configuration, with activation dependent on the HAL's capture/playback mode and reference setup. Static evidence does not establish that ordinary Lineage `AudioRecord` capture receives the processed output.

## ALSA evidence

The device exposes one MediaTek sound card with `MultiMedia1_Capture` (card 0/device 1), `ULDL_Loopback` (0/3), `DL1_AWB_Record` (0/8), `VOIP_Call_BT_Capture` (0/11), `TDM_Debug_Record` (0/12), `MultiMediaData2_Capture` (0/14), and `I2S0AWB_Capture` (0/15), among others. The complete `/proc/asound` output is in `device/`.

The HAL's NEEDED list includes `libtinyalsa.so` and `libtinycompress.so`, and its strings include reference-buffer and VOIP downlink queues. This supports a HAL/ALSA reference path, but no safe mapping from a specific exposed PCM device to `echo_reference` was established.

## Safe next experiment

The smallest reversible next step is static recovery of the HAL's ARM `set_parameters` and capture-open branches with an ARM-aware decompiler, then testing only an already accepted HAL parameter if its exact key/value and lifecycle are proven. A mixer toggle is a secondary candidate only if the HAL itself reads or writes that control. Do not call `asp_*`, `setInVOIPMode`, or echo-reference functions directly until their ABI, ownership, and initialization sequence are recovered.
