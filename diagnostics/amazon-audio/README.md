# Amazon / MediaTek audio investigation

Collected from the reachable Echo Show 5 (`checkers`) on 2026-09-11. The collection is read-only with respect to the ROM, vendor files, audio configuration, and production wake code.

## Static findings

The running image contains the expected Amazon/MediaTek audio stack:

- `/system/lib/libasp.so`
- `/system/lib/libaspclient.so`
- `/system/vendor/lib/hw/audio.primary_amazon.mt8163.so`
- `/system/vendor/lib/hw/audio.primary.amazon_wrapper.so`
- `/system/vendor/lib/libspeech_enh_lib.so`
- `/system/vendor/lib/libaudiocomponentengine.so`
- `/system/vendor/lib/libaudiotoolkit.so`
- `/system/vendor/lib/libaudiosetting.so`

`/vendor/etc/audio-algorithms/` exists and identifies the board as `Checkers`, with two microphones, two speakers, 16 kHz microphone/ASR processing, and 48 kHz speaker processing. `AFE.cfg` explicitly contains enabled AEC and ABF settings, seven source beams, a reference/target beam selector, MICARA, two references, and the `coefs_FBF.cfg` fixed-beamformer coefficient file. `Tap_AEC_mic1.cfg` and `Tap_AEC_mic2.cfg` are present as FIR coefficient files. `MBCL_VOIP.cfg` and the VOIP EQ files are also present.

The extracted `libasp.so` strings contain `echo_reference`, `Enable AEC`, `BeamSelector`, `RefBeamSelector`, `AlignedBeamMerger`, `setInVOIPMode`, AEC adaptation, reference-channel validation, and multi-mic VoIP processing. `libspeech_enh_lib.so` contains `adaptive_beamforming`, `ENH_API_Init_AEC`, `ENH_API_Run_Aec_UL/DL`, `aec_ul_process`, `aec_dl_process`, and speech-enhancement/VOIP symbols. These prove the implementation and its configuration vocabulary are retained. They do not prove that the current Android `AudioRecord` source is routed through every stage.

## SoundTrigger boundary

Android exposes the framework services `soundtrigger` and `soundtrigger_middleware`, but:

- `dumpsys media.sound_trigger_hw` reports `Can't find service`.
- The filesystem search found no `sound_trigger*.so`, hotword, keyword, wakeword, or voice-trigger implementation file.
- `lshal` showed the normal audio/effects factories but no `ISoundTriggerHw` instance.
- `soundtrigger_middleware` shows framework capture-state bookkeeping, not a discovered vendor hardware module.

The evidence currently supports **no verified Amazon/MediaTek hardware hotword path** on this LineageOS image. The framework service names alone are not evidence of Alexa DSP availability.

## Current Android path

The existing `SharedAudioCapture` constructs `AudioRecord` with `VOICE_RECOGNITION`, 16,000 Hz, mono PCM16, and a 6,400-byte-or-larger buffer. The idle and voice-recognition `audio_flinger`/`audio_policy` snapshots are saved beside this file. The original app was restarted after the snapshots; no production wake source, model threshold, or vendor file was changed.

The snapshots show a 16 kHz mono built-in-microphone input profile and no active effect chain for the captured stream. This is a topology observation, not proof that the vendor AFE is bypassed internally.

## Deferred runtime comparison

The requested four-source comparison (`VOICE_RECOGNITION`, `VOICE_COMMUNICATION`, `MIC`, and `UNPROCESSED`), AudioEffect descriptor query, and controlled quiet/speaker-only/speaker-plus-voice leakage recordings were not run before this collection was interrupted. No WAV or PCM comparison should be inferred from the static files.

Those tests require a temporary diagnostic capture mode that logs the AudioRecord session ID, actual route/format, `AudioEffect.queryEffects()` descriptors, active recording configuration, RMS/peak, and microWakeWord scores, while leaving the production `VOICE_RECOGNITION` default unchanged. The user is ready to provide fixed-position speech for both “Hey Jarvis” and “Hey Steve” once that diagnostic mode is installed.

## Files

`commands.jsonl` records each ADB command and exit status. `SHA256SUMS` covers pulled algorithm/configuration/library files. The pulled binaries are under `libraries/`; configuration under `audio-algorithms/` and `configs/`; strings reports are `*-strings-interesting.txt`.
