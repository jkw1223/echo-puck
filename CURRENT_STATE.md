# Echo Puck — current checkpoint

Consolidated September 8, 2026 from source and recorded task history. This is not a fresh device/provider test.

## Resume here

The first-generation Echo Show 5 (`checkers`) IS Puck 0. LineageOS Android 11 supplies the thin hardware endpoint. Active Android sources are in `puckd-android`; the Mac relay/fallback is in `puck-relay`.

The latest work includes direct Android OpenAI Realtime transport, shared microphone capture, ordered response playback, and local wake-word experiments. The previous task reported a successful direct audio probe and an integrated turn, but did **not** establish a clean two-consecutive-turn lifecycle regression. The latest readiness/append/commit correction was built and installed; subsequent input was blocked by Android `NotificationShade` holding focus. Resolve device focus and verify two normal turns before calling the transport accepted.

## Recorded milestones

- September 5: LineageOS/checkers and USB-independent Wi-Fi ADB verified. Original Python web bridge was only compile-checked.
- September 6: acknowledged relay control, local audio, bounded push-to-talk, cancellation/reconnect, and a supervised ChatGPT web text/local-TTS turn verified. A later checkpoint records a successful Mac-relayed OpenAI Realtime turn.
- September 6–7: Tower wake detection, Android openWakeWord/TFLite experiments, native microWakeWord/JNI, and shared AudioRecord ownership explored. Model scores and source presence do not establish a reliable unattended wake lifecycle.
- September 7 evening: direct Android text probe returned expected text; direct audio probe processed streamed audio on a playback worker and drained after `response.done`. Direct transport integration followed, with readiness/order and UI lifecycle corrections. Two consecutive normal turns remain unverified.
- Privacy investigation found `amazon-gating/state` and the red LED transition independently of Android software mute. The attempted `gating.kl` was not selected; `Generic.kl` remained active. Gated-versus-ungated PCM measurements are incomplete.
- Retrieved USB host discussion found no confirmed first-generation checkers success. Kernel/board investigation remains a research lead, not verified host support.

## Configuration and deployment

`MediaTurn` defaults to `transport=relay`; the previous task selected `realtime` in the device's private `puck_status` preferences. Repository consolidation did not re-query or change preferences, install an APK, restart services, flash the Show, or make provider requests.

The development key is read from ignored `puckd-android/secrets.properties` into `BuildConfig.OPENAI_API_KEY`. This embeds it in the APK; secret files, generated sources and APKs stay local. A scoped credential architecture remains future work. The relay has its own ignored `.env.local`.

Historical endpoints: Show `192.168.1.24:5555`; Mac relay `192.168.1.193:8787`; Tower wake service `192.168.1.4:8765`. Recheck these addresses. Relay transport is plaintext LAN HTTP/WebSocket with ephemeral tickets, not durable enrollment.

## Next work

1. Verify installed app, foreground window and transport selection; restore app focus.
2. Run two normal direct turns: check readiness, nonempty append, commit/response ordering, errors, cancellation and playback drain.
3. Validate local wake detection/shared-microphone handoff separately.
4. Finish hardware privacy PCM measurements and device-specific button handling.
5. Camera, full-duplex/barge-in, cold-boot recovery, durable authentication, memory/capability runtime and USB host remain separate uncompleted milestones.

See `docs/WORK_INDEX.md` for provenance. Historical checkpoints under `docs/history/` may contradict later results.
