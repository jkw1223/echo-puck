#!/usr/bin/env bash
set -euo pipefail
# Build the source-pinned standalone candidate. No fetch/gclient invocation.
# Usage: build-android-armv7.sh SOURCE_DIR NDK_DIR [BUILD_DIR]
source_dir=$(realpath "${1:?Supply the standalone source directory}")
ndk_dir=$(realpath "${2:?Supply the official Android NDK r28c directory}")
build_dir=$(realpath -m "${3:-$source_dir/build-android-armv7}")
expected_sha=846fe90a289f58b7c9303a635142aa2c7caa93e5
actual_sha=$(git -C "$source_dir" rev-parse HEAD)
[[ "$actual_sha" == "$expected_sha" ]] || { echo 'Unexpected standalone source revision' >&2; exit 1; }
git -C "$source_dir" diff --exit-code HEAD -- .
python3 - "$ndk_dir" "$build_dir" <<'PY'
from pathlib import Path
import sys
ndk,build=map(Path,sys.argv[1:])
assert '28.2.13676358' in (ndk/'source.properties').read_text(), 'Expected NDK r28c'
build.parent.mkdir(parents=True,exist_ok=True)
bin=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
paths={'c':bin/'armv7a-linux-androideabi24-clang',
       'cpp':bin/'armv7a-linux-androideabi24-clang++',
       'ar':bin/'llvm-ar','strip':bin/'llvm-strip'}
for p in paths.values(): assert p.is_file(), p
text='[binaries]\n'+''.join(f'{k} = {str(v)!r}\n' for k,v in paths.items())
text+="\n[host_machine]\nsystem = 'android'\ncpu_family = 'arm'\ncpu = 'armv7-a'\nendian = 'little'\n\n[properties]\nneeds_exe_wrapper = true\n"
(build.parent/(build.name+'.ini')).write_text(text)
PY
python3 -m mesonbuild.mesonmain setup "$build_dir" "$source_dir" \
  --cross-file "$build_dir.ini" --buildtype=release \
  -Ddefault_library=shared -Dabseil-cpp:default_library=static -Dgnustl=disabled
python3 -m mesonbuild.mesonmain compile -C "$build_dir" -j 2
library="$build_dir/webrtc/modules/audio_processing/libwebrtc-audio-processing-2.so"
file "$library"
"$ndk_dir/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" -h -A -d "$library"
sha256sum "$library"
