# Software AEC build provenance

Status: workstation baseline verified; main WebRTC source checkout is pinned, but standard dependency synchronization was interrupted after repeated multi-gigabyte `src/third_party` clone work. No APM library or diagnostic APK is claimed to work yet.

## Puck repository baseline

```text
repository=C:\Users\Home\echo-puck
branch=main
HEAD=187dc877aeb31f67e3e20c39463b5aa932b309bc
last_commit=187dc87 Refine realtime turn readiness and diagnostics
remote=https://github.com/jkw1223/echo-puck.git
working_tree=dirty before this task; existing modifications and untracked diagnostic files preserved
baseline_tasks=PASS (Gradle wrapper)
baseline_assembleDebug=PASS (Gradle wrapper)
```

## Workstation

```text
host_os=Windows 10.0.26200
host_architecture=Windows host (architecture not yet separately recorded)
adb=Android Debug Bridge 1.0.41, Version 37.0.1-15733141
jdk=openjdk 17.0.20.1 Temurin
git=2.55.0.windows.3
python=not available on PATH
android_sdk=C:\Users\Home\AppData\Local\Android\Sdk
android_ndk=C:\Users\Home\AppData\Local\Android\Sdk\ndk\28.2.13676358
gradle_wrapper=9.6.0
standalone_gradle=not available on PATH
standalone_cmake=not available on PATH; SDK CMake component present
```

## WebRTC

```text
repository=https://webrtc.googlesource.com/src
commit=d9ec82c89d94c319e2a1f547c1bdd58591828455
external_workspace=C:\Users\Home\webrtc-apm-build
depot_tools=C:\Users\Home\depot_tools (revision 36a8df4ad006eaa0572fb446edb8145fe5403592)
license=WebRTC BSD-style license; LICENSE, PATENTS, and AUTHORS must be preserved from the pinned checkout
target_os=android
target_cpu=arm (pending GN generation)
gn=2562 (cfcd774b98f3)
autoninja=1.12.1
```

GN target, exact GN args, Clang version, libsteve_apm.so hash, APK load result, create/destroy result, and synthetic processing result remain pending until synchronization completes and the wrapper is built.

## Blocking evidence

The standard `fetch --nohooks webrtc` / `gclient sync --nohooks --with_branch_heads` workflow cloned the official `src` repository and began the `src/third_party` dependency checkout. The Chromium `third_party` repository advertises approximately 7.82 GiB and the operation repeatedly re-entered that full clone; it was interrupted after about twelve minutes and roughly 12 GB of workspace data. The partial checkout is preserved outside the Puck repository for a later continuation. No fallback dependency was selected.

The resumed sync completed. `gclient runhooks` then failed at:

```text
python3 src/build/vs_toolchain.py update --force
ServiceException: 401 Anonymous caller does not have storage.objects.list access
on bucket chrome-wintoolchain
No downloadable toolchain found.
```

The hook recommends `DEPOT_TOOLS_WIN_TOOLCHAIN=0` when using a locally installed Visual Studio toolchain. That workaround was not applied; GN generation and all APM build work are paused pending an explicit toolchain decision.

## Local Visual Studio prerequisite check

Checked: 2026-09-12.

```text
vswhere.exe: not present at C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe
Visual Studio / Build Tools C++ installation: not found in standard locations
VsDevCmd.bat: not found
cl.exe: not found under standard Visual Studio/Build Tools locations
link.exe: not verified because no local MSVC environment exists
Windows 10 SDK: not found under C:\Program Files (x86)\Windows Kits\10
DEPOT_TOOLS_WIN_TOOLCHAIN=0: not set; hooks were not rerun
```

The approved local-toolchain route cannot proceed until a Visual Studio or Build Tools installation containing the MSVC C++ workload and Windows SDK is available. No installation was attempted without explicit approval.

## Build Tools installation attempt

Build Tools installation was explicitly approved and completed:

```text
product=Visual Studio Build Tools 2022
version=17.14.40 / 17.14.37628.2
install_path=C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools
MSVC toolset=14.44.35207 (compiler 19.44.35228)
cl=C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC\14.44.35207\bin\Hostx64\x64\cl.exe
link=C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC\14.44.35207\bin\Hostx64\x64\link.exe
```

`cl` and `link` were successfully discovered from `VsDevCmd.bat`. However, the Windows SDK payload could not be verified: `C:\Program Files (x86)\Windows Kits\10\Include` and `Lib` contain no SDK version directories. Attempts to add `Microsoft.VisualStudio.Component.Windows10SDK`, `Microsoft.VisualStudio.Component.Windows10SDK.19041`, and the VCTools recommended components completed without materializing SDK headers/libs. The SDK prerequisite remains unresolved.

## Windows SDK verification

Verified: 2026-09-12.

```text
source=https://learn.microsoft.com/en-us/windows/apps/windows-sdk/downloads
installer=Microsoft Windows 10 SDK 19041 standalone winsdksetup.exe
version=10.0.19041.0
WindowsSdkDir=C:\Program Files (x86)\Windows Kits\10\
WindowsSDKVersion=10.0.19041.0\\
Windows.h=C:\Program Files (x86)\Windows Kits\10\Include\10.0.19041.0\um\Windows.h
stdio.h=C:\Program Files (x86)\Windows Kits\10\Include\10.0.19041.0\ucrt\stdio.h
kernel32.lib=C:\Program Files (x86)\Windows Kits\10\Lib\10.0.19041.0\um\x64\kernel32.lib
ucrt.lib=C:\Program Files (x86)\Windows Kits\10\Lib\10.0.19041.0\ucrt\x64\ucrt.lib
```

The Include tree contains `um`, `ucrt`, `shared`, and `winrt`; the Lib tree contains `um` and `ucrt`.

The temporary source `C:\Users\Home\work\sdk-test.cpp` compiled and linked with the fresh Build Tools developer environment, and `sdk-test.exe` ran successfully with output `sdk ok`. The test was outside the Puck repository.

The next hook shell uses the current-session-only settings:

```text
DEPOT_TOOLS_WIN_TOOLCHAIN=0
vs2022_install=C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools
```

## Official hook/tool download recovery

Verified: 2026-09-12.

```text
gclient runhooks=PASS (24/24) with DEPOT_TOOLS_WIN_TOOLCHAIN=0
WebRTC HEAD=d9ec82c89d94c319e2a1f547c1bdd58591828455
GN=C:\Users\Home\webrtc-apm-build\src\buildtools\win\gn.exe
GN version=2562 (cfcd774b98f3)
Ninja=C:\Users\Home\webrtc-apm-build\src\third_party\ninja\ninja.exe
Clang=C:\Users\Home\webrtc-apm-build\src\third_party\llvm-build\Release+Asserts\bin\clang-cl.exe
```

The earlier `gn`/`autoninja` “missing” observation came from invoking the depot_tools wrappers from `C:\Users\Home` rather than from inside the WebRTC checkout. From the correct `src` directory, GN resolves and the official hooks complete. `autoninja --version` is not a supported option for this checkout's Siso-backed wrapper and prints usage; this is not a missing Ninja binary.

## Android GN generation blocker

Attempted: 2026-09-12, from `C:\Users\Home\webrtc-apm-build\src` with the measured ARM target and approved local MSVC environment.

```text
command=buildtools\win\gn.exe gen out\apm-android-arm --args="target_os=\"android\" target_cpu=\"arm\" is_debug=false is_component_build=false rtc_include_tests=false rtc_build_examples=false treat_warnings_as_errors=false"
result=FAIL
file=build/config/BUILDCONFIG.gn:265
assert=assert(host_os == "linux", "Android builds are only supported on Linux.")
error=Android builds are only supported on Linux.
```

The pinned WebRTC revision explicitly rejects Android GN generation on Windows. No source workaround was applied. A Linux build host or approved Linux environment is required for the next native build stage.


## Ubuntu native build preparation (2026-09-12)

Repository freshly cloned and `git pull --ff-only` completed before work:
`195aeff04ad3ed285f561f8328e4ebbb7d08007b` on main. Only four prior
software-AEC records were present: this provenance, device-abi.md,
software-aec-report.md, and webrtc-revision.txt. Other planning files listed in
the handoff were absent; no historical evidence was removed.

```text
host_os=Ubuntu 24.04.4 LTS (Noble)
host_arch=x86_64
kernel=7.0.0-31-generic
external_workspace=/home/user/webrtc-apm-build
depot_tools_origin=https://chromium.googlesource.com/chromium/tools/depot_tools.git
depot_tools_SHA=36a8df4ad006eaa0572fb446edb8145fe5403592
webrtc_origin=https://webrtc.googlesource.com/src.git
webrtc_HEAD=d9ec82c89d94c319e2a1f547c1bdd58591828455
webrtc_checkout=detached HEAD; clean tracked source
fetch_log=/home/user/webrtc-apm-build/fetch.log
```

Initial `fetch --nohooks webrtc` required running `gclient` first to bootstrap
depot_tools Python/CIPD. Bootstrap completed, and the repeated fetch cloned the
main source. The source was explicitly checked out detached at the required
revision and verified using `git rev-parse HEAD`.

The root filesystem is only 30 GiB, initially approximately 11 GiB free and
approximately 9 GiB free after bootstrap/source checkout. Dependency fetching
was deliberately stopped pending the user's offered VM disk expansion. The
checkout is preserved; resume with pinned `gclient sync` after storage is ready.
Android dependency selection must be checked in `.gclient` before sync/hooks.
GN, target inspection, upstream compilation, wrapper compilation, ELF checks,
and artifact SHA-256 remain pending. No native library exists yet. No production
Android source, vendor configuration, or device was modified.

## Full-checkout cleanup and revised strategy (2026-09-12)

The user explicitly stopped the full dependency strategy. Active gclient and
its child processes were terminated and no workspace writer remained before
removal. Before/after reports are saved as `disk-usage-before-cleanup.txt` and
`disk-usage-after-cleanup.txt`. The partial source was approximately 21 GiB;
bootstrapped depot_tools was 1.2 GiB. Both were deleted. The old workspace is
now 44 KiB of logs/configuration. Its resolved path remains
`/media/user/SCRATCHPAD/webrtc-apm-build`; small logs and `.gclient` were copied
into this diagnostics directory before deletion.

The old depot_tools SHA was `36a8df4ad006eaa0572fb446edb8145fe5403592`.
No GN generation or APM compilation was achieved by that route. Earlier
external source paths refer to removed workspaces and are historical evidence.

A new source-only reference is at `/home/user/webrtc-apm-min/webrtc` on
SCRATCHPAD, shallow/partial cloned and detached at the unchanged September
pin `d9ec82c89d94c319e2a1f547c1bdd58591828455`. No fetch/gclient/hook operation
was run on this new workspace. Its initial footprint is 158 MiB.

The preferred standalone alternative is documented in
`minimal-apm-dependency-map.md`; it deliberately has a separate M131 upstream
pin. The repository's historical webrtc-revision.txt is preserved and does not
specify the alternate build source. Use the standalone recipe and pins instead.


## Minimal standalone build result (2026-09-12)

PASS: freedesktop APM v2.1 at `846fe90a289f58b7c9303a635142aa2c7caa93e5`,
upstream WebRTC M131 `79aff54b0fa9238ce3518dd9eaf9610cd6f22e82`.
Official Android NDK r28c 28.2.13676358 / Clang 19.0.1, Meson 1.7.2,
Ninja package 1.11.1.4. Target ARMv7 little endian, API 24, release.
Abseil 20240722.0 and its Meson overlay were hash-verified by Meson.
No tracked standalone source changes. All 431 build steps passed, including
AEC3/NS/AGC2 and the native API example link. No native execution occurred.

Artifact: `/home/user/webrtc-apm-min/standalone/build-android-armv7/webrtc/modules/audio_processing/libwebrtc-audio-processing-2.so`
SHA-256: `c678dd7caa4af5db6882325854885f10f99577ffcd705fd3f6b11ba5e92fa307`.
Size: 1,239,380 bytes. ELF32/ARM/EABI5, ARMv7/Thumb-2/VFPv3/NEONv1.
Dynamic dependencies: liblog.so, libc++_shared.so, libm.so, libc.so.
The C++ runtime will need packaging alongside the library during the later
Android app stage. This is not libsteve_apm.so and has no stable C ABI yet.

The entire minimal workspace uses 2.4 GiB, including NDK and all build outputs.
See the generated target/source inventory, build log, ELF evidence and recipe
under standalone/. API 23 failure evidence is preserved; API 24 resolves its
NDK large-file declaration issue. Production and device state are untouched.
