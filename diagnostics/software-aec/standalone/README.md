# Standalone source build recipe

This candidate uses freedesktop APM v2.1, not the September 2026 upstream API.
See ../minimal-apm-dependency-map.md for its exact upstream import and rationale.
No full WebRTC dependency checkout is used.

## Source and tools

```bash
mkdir -p ~/webrtc-apm-min
cd ~/webrtc-apm-min
git clone --depth=1 --branch v2.1 \
  https://gitlab.freedesktop.org/pulseaudio/webrtc-audio-processing.git standalone
git -C standalone rev-parse HEAD
# Must equal 846fe90a289f58b7c9303a635142aa2c7caa93e5
```

Use official Android NDK r28c (28.2.13676358), Linux archive:
https://dl.google.com/android/repository/android-ndk-r28c-linux.zip
Official repository manifest:
https://dl.google.com/android/repository/repository2-3.xml
Expected archive size: 722261334 bytes; official SHA-1:
`a7b54a5de87fecd125a17d54f73c446199e72a64`.
Validate before extraction and remove the archive afterward to save disk space.

Install Meson 1.7.2 and Ninja Python package 1.11.1.4 into an isolated directory
using a Python environment with pip (no sudo needed):

```bash
python3 -m pip install --no-cache-dir --target "$PWD/build-tools" \
  meson==1.7.2 ninja==1.11.1.4
export PYTHONPATH="$PWD/build-tools"
export PATH="$PWD/build-tools/bin:$PATH"
/path/to/echo-puck/diagnostics/software-aec/standalone/build-android-armv7.sh \
  "$PWD/standalone" "$PWD/android-ndk-r28c"
```

On the initial Ubuntu host, pip was supplied by the bundled Codex Python
runtime; the system Python did not have pip. The Meson/Ninja packages themselves
were installed into the minimal workspace. Meson verifies Abseil source and
build-overlay hashes from the committed wrap file; only those small dependencies
are downloaded. Preserve subprojects/packagecache for offline reproduction.

The recipe uses API 24 and ARMv7, release mode, a shared APM library, and static
Abseil dependencies. The obsolete gnustl dependency is disabled because current
NDKs supply libc++. CPU options are the source project's defaults. Inspect the
recorded ELF attributes for SIMD requirements before choosing other hardware.

The included upstream run-offline native example is compiled/linked as API
integration evidence; it is not executed here. No APK, JNI wrapper or device
installation is part of this stage. The resulting library exposes the upstream
C++ API and is not yet libsteve_apm.so or a stable C ABI.
