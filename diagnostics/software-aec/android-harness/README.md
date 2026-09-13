# Fixture-only Android harness

This separate `com.steve.apmdiag` harness loads the existing ARMv7
`libsteve_apm.so`, allocates direct 160-sample buffers, submits reverse before
capture for 1200 frames, sets an 80 ms delay, and logs lifecycle/errors. It
requests no audio permissions and does not use production Puck code.

Build from a host with JDK 17, Android SDK/NDK and Gradle:

```bash
./gradlew :app:assembleDebug
adb connect 192.168.1.24:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.steve.apmdiag
adb shell monkey -p com.steve.apmdiag 1
adb logcat -d -s ApmDiag:I ApmDiag:E
```

Current VM status: build blocked because no JDK/java executable is installed.
The Echo Show is reachable at `192.168.1.24:5555` and reports Android 11,
`armeabi-v7a`, `armv8l`. No APK was installed and no device audio was used.
The harness still needs fixture file transfer/processing and output pull wired
before numeric device correlation can be claimed.
