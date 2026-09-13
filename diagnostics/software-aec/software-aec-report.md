# Isolated software AEC experiment

## Current status

The workstation baseline is established and the Echo Show ABI is measured. The existing Puck Android project builds successfully without source changes. Official upstream WebRTC source is pinned to `d9ec82c89d94c319e2a1f547c1bdd58591828455` in an external checkout, and standard dependency synchronization has completed. The required WebRTC hooks then failed at the Windows toolchain download step because the anonymous caller lacks access to the `chrome-wintoolchain` Google Cloud Storage bucket.

The experiment has **not** yet demonstrated a built or on-device APM library. No `libsteve_apm.so`, diagnostic APK install, native lifecycle test, or synthetic processing result may be inferred from this record.

## Safety boundary

`com.steve.puckd` was not modified by this task. No Amazon/MediaTek HAL or ASP reverse engineering was performed, and no `dumpsys media.audio_flinger` command was used. No third-party AAR is being used.

## Next gate

MSVC Build Tools and the standalone Windows SDK 10.0.19041.0 are installed and verified, including a successful trivial compile/link/run test. The official WebRTC hooks complete successfully with the approved current-shell local-toolchain settings. GN generation for Android is now blocked by the pinned revision’s explicit assertion that Android builds are supported only on Linux. Physical acoustic testing remains deferred.


## Revised host-only gate completed: standalone APM (2026-09-12)

The full WebRTC dependency checkout was stopped and deleted after saving
provenance and disk reports. The preferred route is now the maintained
freedesktop APM v2.1 source extraction, pinned and cross-built for Android
ARMv7/API 24. Its upstream is WebRTC M131, separately documented from the
unchanged September 2026 reference pin. AEC3, NS and AGC2 are included.

The shared library and native example link successfully; the entire workspace
including NDK is 2.4 GiB. See minimal-apm-dependency-map.md and standalone/ for
pins, license information, exact recipe, target inventory, API notes, hash and
ELF evidence. No full sync is needed or authorized under this strategy.

The source/buildability gate is complete. C ABI wrapper, JNI/APK packaging,
device loading, DSP sanity tests and acoustic AEC validation remain later work.
No production package, vendor audio configuration or device was touched.

## ARMv7 fixture execution gate (2026-09-12)

The deterministic fixture generator and correlation analysis script are now
under `diagnostics/software-aec/`. Fixtures use 16 kHz mono, 12 seconds,
160-sample frames, an 80 ms delayed render-derived echo at -12 dB, and a
deterministic multi-tone desired component. The analysis reports render-
correlated energy and dB reduction, rather than relying on total RMS.

Device execution is **BLOCKED**: `adb devices` currently reports no Echo Show.
Therefore JNI lifecycle, frames processed, output transfer, and numeric ARM
echo reduction are intentionally unclaimed. No live audio or production code
was touched. Once the device is connected, the next step is a fixture-only APK
runner using the existing `libsteve_apm.so` and the recorded scripts.

## SteveApm wrapper layer

The opaque C ABI and thin JNI bridge are under `standalone/wrapper/`. The
wrapper enforces APM's exact native frame count, keeps WebRTC C++ types private,
uses AEC3 (`mobile_mode=false`), NS enabled, and AGC1/AGC2 disabled. Render must
be submitted before capture; JNI forwards direct ByteBuffers and status codes.

The wrapper was cross-built for Android ARMv7/API 24. ELF and SHA-256 evidence
are recorded. The ARM binary has not been executed on this x86 host, so no
synthetic echo-reduction number is claimed yet; fixture and JNI execution remain
the next host/Android harness step. Live Echo Show testing remains out of scope.

## Device/JNI execution attempt (2026-09-12)

The Echo Show is reachable over ADB at `192.168.1.24:5555` and identity matches
Echo Show 5/checkers/Android 11/armeabi-v7a. A separate fixture-only
`com.steve.apmdiag` harness source is preserved under `standalone`-adjacent
`android-harness/`. It requests no audio permissions and does not touch Puck.

APK assembly is currently **BLOCKED** because the Ubuntu VM has no JDK and no
`java` executable (`JAVA_HOME` unset). Consequently no APK was installed, no
JNI lifecycle ran, and no numeric ARM echo reduction is claimed. Add JDK 17 and
rerun the documented harness commands. The existing ARM library hash remains
`9c9ed47307a7bdd94494b26ecf5fc5288e4f09e52cae0638e0aab7e4fa130952`.
