# Isolated software AEC experiment

## Current status

The workstation baseline is established and the Echo Show ABI is measured. The existing Puck Android project builds successfully without source changes. Official upstream WebRTC source is pinned to `d9ec82c89d94c319e2a1f547c1bdd58591828455` in an external checkout, and standard dependency synchronization has completed. The required WebRTC hooks then failed at the Windows toolchain download step because the anonymous caller lacks access to the `chrome-wintoolchain` Google Cloud Storage bucket.

The experiment has **not** yet demonstrated a built or on-device APM library. No `libsteve_apm.so`, diagnostic APK install, native lifecycle test, or synthetic processing result may be inferred from this record.

## Safety boundary

`com.steve.puckd` was not modified by this task. No Amazon/MediaTek HAL or ASP reverse engineering was performed, and no `dumpsys media.audio_flinger` command was used. No third-party AAR is being used.

## Next gate

MSVC Build Tools and the standalone Windows SDK 10.0.19041.0 are installed and verified, including a successful trivial compile/link/run test. The official WebRTC hooks complete successfully with the approved current-shell local-toolchain settings. GN generation for Android is now blocked by the pinned revision’s explicit assertion that Android builds are supported only on Linux. Physical acoustic testing remains deferred.
