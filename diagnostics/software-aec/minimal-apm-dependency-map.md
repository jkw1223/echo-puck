# Minimal APM dependency map

## Strategy and pins

The full depot_tools/gclient route was abandoned at the user's direction on
2026-09-12. No further `fetch webrtc` or `gclient sync` is permitted by this
stage. See the before/after disk reports. The old source and bootstrapped
depot_tools trees were removed after stopping their process tree.

Source-only reference checkout:
`/home/user/webrtc-apm-min/webrtc`, resolving under SCRATCHPAD.
Official origin: https://webrtc.googlesource.com/src
Pin: `d9ec82c89d94c319e2a1f547c1bdd58591828455`.
Cloned with `--depth=1 --filter=blob:none --no-checkout`, fetched the exact
commit with `--depth=1`, then checked out detached. No dependency sync or hooks.
Measured initial footprint: 158 MiB.

Preferred build candidate: freedesktop/PulseAudio `webrtc-audio-processing`
v2.1, commit `846fe90a289f58b7c9303a635142aa2c7caa93e5`.
Origin: https://gitlab.freedesktop.org/pulseaudio/webrtc-audio-processing.git
This is an independent source extraction with a Meson build; it does not
require the PulseAudio server or any network service at runtime.

Its import commit `b5c48b97f653e1b3d62bc484bde7372f91397eac` explicitly records:

- WebRTC `79aff54b0fa9238ce3518dd9eaf9610cd6f22e82`
- Chromium `2a19506ad24af755f2a215a4c61f775393e0db42`
- Chromium release `131.0.6778.200` (M131)

This is **not** the September 2026 reference pin. It is the preferred alternate
source under the revised strategy. Its COPYING is BSD-3-Clause; upstream
third-party notices remain applicable. The release NEWS and tracked patches/
explain platform/build changes. The original API discovery document describes
the September pin and must not be treated as the standalone API contract.

## September pin: static source inspection

These are source-inspected GN labels/dependencies, not generated GN results.
All paths are relative to the pinned WebRTC reference tree. BUILD.gn contains
conditional branches and templates; this inventory does not claim an exact
compiled closure or a mathematically smallest file set.

| Component | Entry point | Required source families |
|---|---|---|
| AudioProcessing | `api/audio:audio_processing`, `api/audio:builtin_audio_processing_builder`, `modules/audio_processing:audio_processing` | API/config/stats, ref counting, environment and task-queue abstractions, APM implementation, buffers/frame proxies, RMS, HPF, post-filter, capture-level adjustment, AEC3, NS, both gain controllers, VAD, AEC dump interface, common_audio, rtc_base synchronization/logging/checks/time, system_wrappers metrics, Abseil |
| AEC3 | `modules/audio_processing/aec3:aec3` | Adaptive FIR/filter ERL, 64-sample blocks, render buffers/delay estimation, FFT data, matched filter, subtraction, echo/reverb/noise estimation, suppression, API echo control/config/environment, APM buffers/logging/HPF, Ooura 128-point FFT, biquad utility, CPU/architecture helpers, field-trial parser, metrics, Abseil string_view |
| NS | `modules/audio_processing/ns:ns` | Noise/signal/prior/quantile estimation, histograms, speech probability, Wiener/suppression parameters, NS FFT, audio_buffer, Ooura 256-point FFT, checks |
| AGC2 | `modules/audio_processing:gain_controller2` and `modules/audio_processing/agc2:*` | Adaptive/fixed gain, limiter, input-volume/clipping controller, speech/noise/saturation estimation, gain maps, VAD wrapper, RNN VAD, CPU dispatch, frame API, common_audio, logging/checks/metrics |

`common_audio/BUILD.gn` covers resampling, signal-processing primitives,
channel conversion, FIR/FFT and architecture implementations. `rtc_base`
provides infrastructure, not just DSP: synchronization, checks, logging,
reference counts, buffers, time and CPU feature detection. `api/environment`
construction introduces default utility implementations if using the current
builder API. Merely copying four top-level directories is insufficient.

External DSP/infrastructure families include Abseil, PFFFT and RNNoise model
weights where referenced. Protobuf is conditional for AEC dump implementation;
a null dump factory is available when it is disabled. ARM/NEON and x86 AVX2
source branches must be selected for the target, not indiscriminately compiled.

The root DEPS is a development-environment manifest, not an APM link manifest.
It includes Chromium build/buildtools/third_party histories, Gradle, test trees,
SDKs and tooling. Fetching it wholesale caused the excessive footprint. None
of Chromium networking, browser rendering, Java/Gradle, video codecs, gRPC,
ICU or the full Android WebRTC SDK is inherently required by this DSP API.

## Standalone compilable closure candidate

The maintained Meson source lists select:

- `webrtc/api`, `webrtc/rtc_base`, `webrtc/system_wrappers`
- `webrtc/common_audio` including local Ooura and signal-processing code
- `webrtc/modules/audio_processing` including AEC3, NS, AGC/AGC2
- local `webrtc/third_party/pffft` and `webrtc/third_party/rnnoise`
- one external source dependency: Abseil 20240722.0

The committed `subprojects/abseil-cpp.wrap` pins both its source archive and
Meson overlay using SHA-256. Source hash:
`f50e5ac311a81382da7fa75b97310e4b9006474f9560ac46f54a9967f07d4ae3`.
Overlay hash:
`12dd8df1488a314c53e3751abd2750cf233b830651d168b6a9f15e7d0cf71f7b`.
Abseil uses Apache-2.0, compatible with this BSD source distribution when its
notices are preserved. No mystery prebuilt APM is involved.

The Meson build explicitly handles `host_machine.system() == 'android'`, links
Android liblog, defines WEBRTC_ANDROID/WEBRTC_LINUX/WEBRTC_POSIX, and detects
32-bit ARM and ARMv7 compiler macros. Its NEON feature selects architecture
sources. Android ARMv7 still requires actual cross-build verification; source
support alone is not proof of a working artifact.

## Alternatives

1. Prefer the maintained standalone extraction, pinned above: small source,
   auditable upstream import and build lists, BSD license, AEC3, native C++ API.
2. Locally extract the September pin only if the older standalone API/DSP proves
   unsuitable. This requires maintaining a new source closure/build and testing
   every architecture branch; it is not yet a verified build route.
3. Full official checkout: explicitly disallowed unless the user intentionally
   revisits storage and strategy. Do not retry it as an automatic fallback.

## Verification status

Cross-build **PASS**: all 431 build steps completed with the committed recipe.
The library and native example link for Android ARMv7/API 24.
Final workspace footprint including NDK, source, dependencies and build: **2.4 GiB**.
The abandoned workspace is 44 KiB. See standalone/artifact-verification.txt.

The API 23 attempt failed because Meson enables `_FILE_OFFSET_BITS=64` and
NDK fseeko64/ftello64 are introduced at API 24. Using API 24 fixed this without
patching any DSP or Abseil source; it remains below Android 11/API 30.
ELF attributes show ARM v7, Thumb-2, VFPv3 and NEONv1. Runtime hardware support
and actual device loading are not tested.

The shared library is 1,239,380 bytes and has SHA-256
`c678dd7caa4af5db6882325854885f10f99577ffcd705fd3f6b11ba5e92fa307`.
Its SONAME is `libwebrtc-audio-processing-2.so`; dynamic dependencies are
`liblog.so`, `libc++_shared.so`, `libm.so`, `libc.so`. Abseil is static.
Defined dynamic symbols include AudioProcessingBuilder::Create, EchoCanceller3,
NoiseSuppressor and GainController2. This is a C++ source/library candidate,
not a completed SteveApm C ABI or JNI component. The target for the entire
minimal workspace, including the Android toolchain, is below 10 GiB. No device,
production app, Amazon audio stack or audio_effects.xml is changed.
