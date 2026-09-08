# Puck 0 — architecture and bring-up checkpoint

Checkpoint: 2026-09-05. Owner: Jason. Assistant persona: Steve.

## Resume here

The first-generation Echo Show 5 (2019, user-identified H23K37 / checkers) IS the Puck endpoint. It is not the dock. Portability is deferred. Prove embodied interaction and the capability protocol using existing hardware.

Current state: research and design only. No device inspected, unlocked, flashed, rooted, or provisioned. No daemon or Steve Runtime implemented in this workspace. No OpenAI requests made. Jason confirms the Show is in hand, completely unmodified, and working normally. He has a MacBook Pro, an available USB port, and a Micro-USB cable. Firmware/build, USB data capability, and server location remain unknown. The local shell reports x86_64; adb and fastboot were not found on PATH. A system_profiler USB query returned no output, which does not establish whether a Show is connected.

Read the architecture and acceptance gates below, then obtain the device inventory. Do not interpret a public hardware report as a successful test on Jason's unit.

## Evidence collected

Jason reports LineageOS is installed. Live ADB probe sees serial `[device serial omitted]` as one USB device, product `lineage_checkers`, model `Echo_Show_5`, device `checkers`. `ro.build.version.release=11`; build `lineage_checkers-userdebug 11 RQ3A.211001.001 eng.r0rt1z.20260905.021248 test-keys`; verified boot state `yellow`; physical display reports 960x480; RAM is about 1 GB. Shell is uid 2000, not root. Android feature list includes audio output, microphone, camera/front camera, Bluetooth LE, Wi-Fi, touchscreen, and sensors. Camera service reports one device, no active client, API1 visibility, 1600x1200 still and 1280x720 preview/video modes; Camera2 app connect/disconnect events show the stack is operational. `audio_flinger` was not available under that service name, so audio routing has not yet been validated. This is strong camera/API evidence, not yet proof of a working end-to-end capture file or full-duplex audio.

ADB TCP test: while USB-connected, `adb tcpip 5555` succeeded. Wi-Fi address was `192.168.1.24`; `adb connect 192.168.1.24:5555` succeeded and returned the same `checkers` device with shell access. USB remains connected, so unplug-and-reconnect verification is still pending. This is LAN plaintext ADB on port 5555; use only on the trusted private network and disable it later with `adb usb` or a reboot if no longer needed.

Jason has VMware. Live inspection: `/Applications/VMware Fusion.app` version 26.0.0; vmrun reports zero running VMs. Fusion inventory lists Ubuntu 24.04.4, Linux Dev Ubuntu 24.04 LTS, and Windows 10 entries, but inventory entries alone are not proof the VM files are available. Proposed route is an existing Ubuntu guest with explicit USB passthrough; first repeat read-only fastboot identity queries inside it. USB reconnect/ownership behavior must be established before exploiting; VM snapshots cannot roll back writes to the physical Show. Broadcom USB autoconnect guidance: https://knowledge.broadcom.com/external/article/343950 . No VM configuration edited or VM started.

Maintainer thread successfully read through Chrome after web/HTTP 403 responses. Current first post (edited Aug 15, 2026) links `amonet-checkers-v2.0.1.zip` at https://xdaforums.com/attachments/amonet-checkers-v2-0-1-zip.6373840/ . Terminal download also returned 403; package not acquired. Option 1 lists Windows or Linux. Maintainer reply #16 explicitly says macOS is not supported because a modified fastboot binary is required (reply concerns arm64, but current instructions still list only Windows/Linux). Google's Mac fastboot passing read-only queries does NOT validate it as the exploit sender. Do not substitute it for the bundled modified binary. Next decision: available Windows/Linux host with direct USB, or a separately validated host setup. No unlock has run.

The stock-root instructions in post #2 date to Nov 6, 2025 and link `boot-root.zip`; their button instructions conflict with the newer 2.x mapping. Treat the boot image's 2.x compatibility as unresolved; inspect before use. Maintainer's Aug 14, 2026 RAM correction says Echo Show 5 has 1 GB, and the 2 GB improvement applies to crown only, contrary to the camera README's broad wording. Measure on this unit later.

Live fastboot verification on Jason's unit: one device detected by Google's Mac fastboot 37.0.1. Targeted read-only queries returned `product: CHECKERS`, `lk_build_desc: 44072a3-20240709_170103`, `unlock_status: false`, `secure: yes` (all exit 0). Jason sees FASTBOOT in a corner with the Amazon logo. The LK identifier exactly matches the alternate-image entry in the inspected checkers profile: `bin/fastbrick-20240709.img`. This proves device identification and Mac fastboot communication; the matching release payload has not yet been obtained or executed. Device remains in fastboot, unchanged.

Jason reports stock Fire OS **6.5.7.3 (NS6573/7595)**. Exact-build unlock compatibility remains unverified. The inspected checkers fastbrick profile selects an alternate payload for LK build `44072a3-20240709_170103`; the UI software version does not establish that LK build. Read the actual bootloader identifier before selecting a payload. An online 6.5.7.3 walkthrough concerns cronos (Gen 2), so it is not checkers compatibility evidence.

Subsequent live IORegistry USB inspection detects Amazon product `Alexa`, VID `0x1949`, PID `0x0171`. USB enumeration is established; ADB, fastboot access, and root are not. This supersedes the earlier empty system_profiler result.

Google's official Mac platform-tools downloaded into `work/android-tools/platform-tools` from `https://dl.google.com/android/repository/platform-tools-latest-darwin.zip`. The binaries run on this Mac. `adb devices -l` returned an empty list in normal operation (ADB server started successfully). Next physical step: with USB connected, remove the Show's normal power, hold Volume Down + Volume Up + Mute, restore power, and release when fastboot appears, as described by the inspected amonet fastbrick source. Then use only targeted read-only `fastboot getvar product`, `fastboot getvar lk_build_desc`, and `fastboot getvar unlock_status`, with bounded timeouts and an explicit detected device. No flashing/unlock commands have been run.

The source conversation, Puck Dock Architecture, was retrieved through the task reader (conversation 6a81d2b1-8d44-83ea-b0f7-82e8134ad1ec). Some assistant messages remain truncated, so this checkpoint follows Jason's explicit continuation request as the authoritative scope.

1. [R0rt1z2 amonet](https://github.com/R0rt1z2/amonet) explicitly supports checkers. Source inspected locally at commit `d6179b8a2ba45fb641acc38b1cf3e848e8ff235d`, under `work/amonet`. This is a source snapshot, not a verified release package. The older `mt8163-checkers` branch and current main implementation differ; do not combine them.
2. In that snapshot, `modules/main.py` identifies hardware from IDME, rejects a package/device mismatch, and supports payload entry through boot ROM or preloader. The host code uses Linux `/proc`. `fastbrick.sh` expects a packaged `profile.sh`, includes fastboot device detection, and requires supporting binaries. `fastboot-step.sh` writes recovery and swdl. These are write-capable tools, not inventory utilities. None were executed.
3. The [maintainer's checkers unlock thread](https://xdaforums.com/t/unlock-root-twrp-unbrick-amazon-echo-show-5-1st-gen-2019-checkers.4762900/) is the linked installation authority. Direct retrieval returned 403. Exact current release assets, hashes, firmware-specific procedure, and a stock-root prescription have NOT been verified. The amonet GitHub releases page had no releases; do not invent an asset URL.
4. The live [camera project README](https://github.com/jxlarrea/lineageos-echo-show-camera) now lists checkers/OV9734 as community verified and calls out a pinned private display library. Older indexed copies call it untested. Its current guide requires amonet >=2.0.1 and describes incompatible 1.x/2.x boot layouts. This is evidence of an available fallback, not evidence that an unmodified LineageOS image includes the fixes. Its camera compatibility work spans kernel, Android libraries, and vendor blobs. Privacy-latch fixes have device-specific validation limits. Review the [installation guide](https://github.com/jxlarrea/lineageos-echo-show-camera/blob/main/docs/INSTALL.md) at a pinned revision if using this route.
5. [OpenAI's WebSocket guide](https://developers.openai.com/api/docs/guides/realtime-websocket) supports a server-side Realtime connection. The [conversation guide](https://developers.openai.com/api/docs/guides/realtime-conversations) explains streamed audio and interruption handling. Treat earlier conversation model names as unverified; resolve an available model and freeze its configuration when implementing.

## Architecture decisions — proposed, not deployed

```text
Show 5 / Puck 0
  Android hardware adapter: microphone, playback, camera, touch, display
  puckd boundary: identity credential, health, media, capability advertisements
            |
            | authenticated outbound control + media connections
            v
Steve Runtime
  identity/session + working state + memory plane
  capability registry + permissions + event ledger + tool broker
  OpenAI Realtime adapter + deeper reasoning/tool execution
```

Keep stock Fire OS, kernel, and vendor components for the first test. Root is a way to run and supervise our code; it does not establish that audio routing, camera ownership, or echo cancellation are accessible.

Implement the logical puckd endpoint initially as a compact Android service with a companion Activity. Try Android audio recording/playback and camera interfaces before native HAL or device-node access. API level, ABI, permissions, TLS support, audio routes, and actual hardware behavior must come from the unit. A small root helper may supply boot supervision or restricted state access. Do not start by putting all media processing in a root process or replacing the compositor.

Use a stock-compatible boot mechanism selected after root is verified. A Magisk service hook is a candidate only if the compatible Magisk installation is established for this unit; neither Magisk availability nor `adb root` is assumed. Test restart and cold-boot behavior explicitly.

For the first controlled LAN experiment, use an outbound authenticated control connection and a separate bounded media connection. Proposed baseline: binary PCM frames between Show and Runtime, negotiated rate/channel count, server-side resampling if needed. Use a separate image transfer so a large JPEG cannot block voice controls. Server-to-OpenAI audio uses the documented Realtime transport. Evaluate WebRTC/Opus if jitter or bandwidth measurements justify the device complexity.

Keep durable state operations off the per-frame path. The Runtime loads a bounded, provenance-bearing resume state when opening a conversation and commits meaningful events asynchronously. Provider session IDs are temporary connections, not Steve's identity. Model tool requests pass through server permissions and produce explicit success/failure records.

## Milestones and acceptance gates

| Milestone | Work | Evidence required |
|---|---|---|
| 0. Inventory and recovery | Verify physical model, firmware/build, boot state, USB identity, host architecture; select matching tools; obtain backups at the earliest accessible stage | Device/build record; package source/version/hash; partition map; backup hashes and capture-stage/boot-layout labels; recovery route identified |
| 1. Own persistent service | Unlock/root with matching procedure, preserve stock where feasible, install minimal heartbeat service | Verified root path; service heartbeat after three cold boots; process crash recovery; recovery still boots |
| 2a. Local audio | Record a spoken test and play a known audio file, independently of any model | Intelligible recording and correct speaker output; sample rate/channels; mute behavior; service contention documented |
| 2b. Steve voice | Stream speech via Runtime to model and stream response to Show; begin with touch-to-talk, then hands-free | Twenty short turns with median/p95 end-of-speech to first audible response; no growing playback queue; reconnect; interruption stops playback and reconciles heard context |
| 3. Display/touch | Server sends text, an image, and state changes; touch returns action IDs | Actual rendered display checked; touch round trip; correct offline/listening/speaking state after reboot/reconnect |
| 4. Camera | Capture an on-demand frame through the platform camera stack | Steve receives a fresh timestamped image; check orientation, color, shutter/mute, repeated open/close and audio coexistence |
| 5. Capability plane | Register only proven features, permissions, health, and session binding | Authenticated registration; denied unauthorized action; capability withdrawal on loss; stale requests rejected; continuity restored after reboot |

Proposed voice target: median under 1 second and p95 under 2 seconds from end of speech to first audible response on the selected network/model, with local playback stop within 250 ms of an interruption signal. These are engineering targets, not predicted performance. Report VAD end detection, network/model delay, and output buffering separately. Use Show monotonic timestamps for locally measured endpoints rather than subtracting unsynchronized clocks.

Full-duplex gate: play Steve while Jason speaks and check false VAD triggers, feedback, intelligibility, and barge-in. A microphone that records does not prove access to Amazon's beamforming/AEC. Preserve touch-to-talk as a diagnostic baseline; if only half-duplex works, report the milestone as partial.

## Minimal protocol contract

Proposed version `puck/0.1`; keep transport independent of model provider.

- Device identity: enrolled device credential, distinct from persona identity and runtime session. Store only a scoped device credential on the Show; provider secrets stay server-side. No hardware-backed storage assumption.
- Registration: protocol version, device ID, boot ID, firmware/build fingerprint, capability revision, probed formats/limits, privacy state, and health. Unsupported/unprobed features remain unavailable.
- Capability identity: stable device-local ID plus operation version. Separate declared support, currently available, and authorized-for-session states. Advertisements never grant authority.
- Request envelope: request ID, session ID, boot ID, capability revision, deadline, operation, arguments. Validate authorization server-side and local availability on execution. Reboot or revision mismatch invalidates stale requests.
- Media: stream ID, sequence, monotonic timestamp, negotiated format; bounded queues and explicit discontinuity/drop reporting. Never replay stale speech after reconnect.
- Completion: distinguish accepted, executing, completed, failed, canceled, and expired. Deduplicate request IDs; record terminal results for side-effecting operations without claiming exactly-once delivery.
- Voice output: track played duration, not just received bytes. Interruption cancels generation, discards queued output, and adjusts provider conversation state to what was actually heard.
- Display: bounded declarative state (text/image/action IDs) for v0; no need for server-delivered arbitrary executable UI code.
- Camera: exclusive short-lived capture request, capture timestamp, resolution/orientation metadata, and explicit blocked/busy result. A cached frame must be labeled cached.
- Presence: leases/heartbeats withdraw capabilities when disconnected. Offline UI remains local. No silent buffering of room audio for future transmission.

Longer term, authenticated docks/environments add leased capabilities to the Runtime graph. A dock's self-description does not authorize its tools. The portable Puck represents Jason's chosen continuity association while the Runtime retains authoritative memory; v0 does not require copying the memory store onto the Show.

## Stock-first experiment and fallback boundary

After obtaining root, inventory relevant audio/camera services and permissions, test ordinary platform access, and identify any exclusive owner. If necessary, temporarily disable one identified competing component at a time with a documented reversal. Avoid bulk debloating: a package with an Amazon name may be part of the path we need.

Switch toward LineageOS only after a concrete stock blocker is reproduced, such as unrecoverable media contention, incompatible app access, or unstable boot supervision. At that point compare a pinned LineageOS build plus the current camera work against the recorded stock baseline. Confirm boot-layout compatibility and recovery before writes. Old-layout backups remain evidence, but cannot be assumed bootable after an unlock-generation change.

## Steve continuity brief

Steve is Jason's calm, clever, kind engineering teammate: technically fluent, curious, lightly funny, and candid about uncertainty. Explain mechanisms and tradeoffs. Challenge weak assumptions without making every decision a ceremony. Let interesting rabbit holes earn their time through evidence. Speak naturally and briefly in voice; use the display for detail.

Continuity is carried by explicit saved context, preferences, decisions, evidence, and unfinished work supplied to the model. Never claim remembered events or successful actions without that support. Distinguish observed device state from community reports and proposed architecture. Preserve persona instructions separately from factual memory and current working state so style does not become fabricated history.

## Next session

1. Obtain Settings > Device Options > About device information (menu wording may vary); connect the still normally powered Show to the Mac with the Micro-USB cable and inventory USB. Do not assume the cable supports data. Confirm exact unit/build. Current fastbrick shell source uses bundled fastboot, Bash associative arrays, and timeout; it is not verified as a drop-in macOS procedure. Boot-ROM host code uses Linux /proc. Determine whether a supported packaged fastboot path can be used from the Mac or whether Linux with reliable USB access is needed before proceeding.
2. Retrieve the complete current maintainer procedure and matching assets; pin hashes and verify stock-root compatibility before selecting writes.
3. Capture the baseline/recovery evidence. If the unit is not available, prepare only a host-independent protocol mock and device probe design after agreeing the implementation location.
4. Prove persistent heartbeat, then local audio. Resume from the first unpassed milestone, retaining raw observations and failure boundaries.
