# Puck 0 — current checkpoint

Updated September 6, 2026, approximately 12:47 PM America/Chicago.

## Resume here

The two-way media bridge is built, installed, and tested on the physical Echo Show 5. A supervised ChatGPT web bridge has now completed one real voice turn. Jason tapped Talk, spoke, tapped Finish, and confirmed that the Mac-supplied test voice was clear and its caption appeared. The first media turn used a fixed local fixture. The next supervised turn used ChatGPT web response text and Mac-local speech synthesis. The unattended browser adapter remains the next missing component.

Do not repeat unlock, flashing, or the initial local audio bring-up. September 5's original checkpoint has obsolete opening/next-session instructions. The prior September 6 control/audio checkpoint is preserved in `artifacts/2026-09-06-media-bridge/before/CURRENT_STATE.md`.

## Actual running topology

- Echo Show 5 Gen 1 (`checkers`), LineageOS Android 11; Wi-Fi ADB `192.168.1.24:5555`.
- Android app `com.steve.puckd`; launch `.MainActivity`, which starts the private `.PuckService`.
- Mac relay: `ws://192.168.1.193:8787/puck`; health `http://192.168.1.193:8787/health`; HTTP media under `/turns/` on the same port.
- Source: `/Users/jason/puck/puckd-android` and `/Users/jason/puck/puck-relay`.
- Installed APK: `puckd-android/app/build/outputs/apk/debug/app-debug.apk`.
- Relay runs as a foreground Node process launched by Codex; no login/boot supervision is installed. Inspect port 8787 before starting a second relay.

## Implemented behavior

1. Talk records mono PCM16 at 16 kHz, using the previously verified Android microphone path. Finish ends capture; a 30-second maximum also ends capture. Stop or leaving the Activity cancels it.
2. The app posts the finished audio to the Mac on a separate HTTP connection. Each turn has a UUID and is bound to its live WebSocket connection via a random ephemeral ticket plus service session ID.
3. Mac acknowledges the turn ID, exact received bytes, and SHA-256. The Show validates all three before requesting a reply.
4. The Mac returns a caption and 4.533375-second spoken fixture: “Jason, your recording reached the Mac. This is the Puck bridge test reply.” Generated locally using macOS `say`; no provider/API call.
5. The Show displays Speaking / bridge test, renders the caption, and plays the returned PCM through its speaker.
6. Stop cancels local capture, HTTP calls, and playback and requests server-side cancellation. Connection loss changes the ticket and cancels the turn. No audio upload is retried; a reply can be fetched only once. Failed/expired turns require a new tap.
7. Heartbeats remain separate from media. The control path retains acknowledged heartbeats, 8-second ACK timeout, bounded retry, and stale-socket isolation.


## Supervised ChatGPT bridge evidence

- Local Whisper smoke test passed using `/Users/jason/Documents/Whisper/whisper.cpp/build/bin/whisper-cli` and `ggml-base.en.bin`; known reply transcription completed in approximately 1.5 seconds on CPU.
- Browser task: existing Chrome tab `https://chatgpt.com/c/6a9c74ae-5908-83ea-ab3f-b8abfca171b1` titled “Puck Dock Feasibility Plan”, visible model label `GPT-5.6 Luna Light`.
- Real Show turn `d66fadc9-18ec-4d93-86a1-17a61437c31a`: 72,960 frames / 145,920 bytes / 4.56 s, local transcript `Hey Steve, can you hear me from the show?`, input SHA-256 `c9ef29eba43bff03376e635c1e5cc53fa0547c4d223233ca77226bc8689225b6`.
- Browser operator submitted that exact transcript with the Steve/Puck continuity brief. ChatGPT returned: `Yes, Jason—your words reached me through the Show and the Mac bridge. I didn’t directly hear the recording, but the transcription came through clearly.`
- Operator returned the exact response to the relay. Mac `say` plus `afconvert` produced 266,748 PCM bytes / 133,374 frames. The Show completed playback; Jason confirmed the audio and matching caption were clear.
- Relay logs show `browser_turn_ready`, `browser_reply_ready`, and `turn_reply_sent` with `source: chatgpt_web`. Operator job queue is empty after completion.
- The queue is intentionally supervised: the browser operator must claim a transcript, submit it in the verified ChatGPT conversation, and post the returned text. There is no CDP/page automation daemon, direct ChatGPT account API, or OpenAI API key in this project.
- Operator token is generated under `puck-relay/.runtime/operator-token` with mode 0600; the operator HTTP server binds to loopback port 8790. It is not a durable credential or device enrollment mechanism.

## Verified evidence

- Debug build and Wi-Fi APK installation succeeded.
- Ten Node tests pass: four existing control tests and six media tests covering byte/hash receipt, one-time replies, live-ticket/session validation, canceled-turn withdrawal, cancel-before-upload ordering, size/format/concurrency rejection, obsolete-connection rejection, and incomplete-upload cancellation.
- Tests found a close-handshake race where a ticket briefly survived client closure. Media now requires an actually OPEN WebSocket, as well as the matching ticket/session; test passes after correction.
- First real turn: `b9db2b95-41a0-4827-8353-f10ff937d43a`, 93,760 mono samples = 187,520 bytes = 5.86 seconds.
- Mac and Show independently reported SHA-256 `89ee8908954f6d0a11f9489c67a06950b740e41b0723513949c9df9fece4449c` for that upload.
- Returned fixture was 145,068 PCM bytes; AudioTrack reached 72,534/72,534 frames and logged `turn_complete`.
- Jason explicitly confirmed hearing the reply clearly and seeing the caption.
- Recording canceled with Stop: relay's received-turn count stayed unchanged; no upload occurred.
- Reply canceled with Stop: live UI was in Speaking state with the returned caption; Stop changed it to Stopped, AudioService released the player, and that turn did not log playback/turn completion.
- Relay terminated during recording: Show displayed Offline and Stopped, Talk was disabled, and the recording was discarded. Relay restarted and Show reconnected at 12:08:29 with a fresh media ticket. More than six subsequent heartbeats passed; no canceled recording was uploaded or reply replayed (received-turn count remained two).
- Earlier slice separately verified graceful restart and stalled-socket recovery, local microphone clarity, and background audio cancellation.

## Data and authority boundaries

- This is still a LAN prototype using plaintext HTTP/WebSocket. Ephemeral media tickets associate a transfer with a live connection; they are not persistent device enrollment/authentication and do not encrypt traffic.
- Captured speech is sent only after Finish (or the 30-second cap). It is held in memory for the transfer; the relay logs receipt metadata/hash and does not save or transcribe the speech. No speech is sent to OpenAI or another provider.
- The returned voice is the same pre-generated test clip each time. Its statement confirms a received payload, not speech understanding.
- No microphone is left continuously open; final device state should be Connected with microphone off.

## Files and evidence

- `puckd-android/app/src/main/java/com/steve/puckd/MediaTurn.kt`: capture, HTTP exchange, receipt validation, reply playback, cancellation.
- `PuckService.kt`: control WebSocket and ephemeral media-ticket lifecycle.
- `MainActivity.kt`: Talk/Finish/Stop UI and connection-state handling.
- `puck-relay/media.js`: bounded HTTP turn protocol, ticket association, receipt, fixture response, expiry/cancel.
- `puck-relay/fixtures/test-reply.pcm` and `.wav`: known local speech fixture.
- `artifacts/2026-09-06-media-bridge`: build/test/device/relay logs, screenshots/UI trees, and prior source/APK backups. These are application backups, not firmware backups.

## Next work

1. Replace the supervised browser handoff with a deliberately chosen automation boundary, if desired. The current queue proves transcription and response plumbing but requires this Codex/browser task to operate the visible ChatGPT conversation; it must not claim to be unattended.
2. Keep the fixed fixture path as the diagnostic fallback and retain the working push-to-talk media path while improving browser orchestration.
3. Supply a bounded Steve identity/project continuity brief to the chosen conversation. Full memory/runtime/capability-plane implementation remains deferred.
4. Camera, remote model-driven display, hands-free/full-duplex interaction, persistent enrollment/TLS, and device/host cold-boot recovery remain unproven.

The Show IS Puck 0. The Mac is the current host. Windows 11 NUC and Pi 4 are previously discussed future hosts/infrastructure, not part of this deployed slice.

## 2026-09-06 — direct OpenAI adapter staged

- Added `puck-relay/openai-provider.js`: bounded turn provider that locally transcribes Show PCM with Whisper, sends only the transcript plus Steve's system prompt to the OpenAI Responses API, then synthesizes reply audio locally with macOS `say`/`afconvert`.
- `puck-relay/index.js` now loads `puck-relay/.env.local` in memory and enables the adapter with `PUCK_REPLY_MODE=openai`; the API key never goes to Android.
- `.env.local` is ignored by git. The expected file is currently absent in this checkout, so live OpenAI mode has not been started or live-tested.
- Relay tests: 15 passed, 1 timing-sensitive heartbeat lease test failed once; rerun needed before calling the suite clean.

## 2026-09-06 — Realtime provider staged

- Added `puck-relay/realtime-provider.js` and `PUCK_REPLY_MODE=realtime`.
- The relay opens `wss://api.openai.com/v1/realtime`, configures 16 kHz PCM input and 24 kHz PCM output, appends each captured turn, commits it, requests a response, collects streamed audio/transcript events, and resamples output to the Show's 16 kHz PCM format.
- The Show still uses the existing tested push-to-talk turn boundary; the upstream model transport is now Realtime.

## 2026-09-06 — first successful OpenAI Realtime turn

- After removing the obsolete `OpenAI-Beta: realtime=v1` header required by the GA endpoint, the Show completed a live turn through Realtime.
- Turn `90df1163-f3bc-478a-9d5c-e31226042b05`: 62,080 input bytes / 1.94 seconds; reply 132,800 bytes; relay source `openai_realtime`.
- The Show remained connected and heartbeating after playback.
