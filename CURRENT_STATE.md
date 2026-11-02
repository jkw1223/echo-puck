# Echo Puck — current checkpoint

Updated November 2, 2026 after direct Echo Show testing and source cleanup. Device observations below are from this session; the source cleanup has not yet been rebuilt or installed.

## Active architecture

The first-generation Echo Show 5 (`checkers`) is Puck 0. The Android app captures audio and runs local wake detection on the Show, connects directly to OpenAI Realtime over TLS, and plays streamed responses through the Show speaker. No audio turn is routed through the Mac or Tower. Retired relay implementations and obsolete wake experiments were removed from the active source tree; historical notes remain in `docs/history/`.

## Latest device evidence

- The Show was reachable at `192.168.1.24:5555`, running Android 11 and `com.steve.puckd/.MainActivity`.
- The active app was configured for direct Realtime, voice `cedar`, and local microWakeWord. Its displayed `Offline` state belonged to the old relay status and did not block direct turns.
- The direct text probe opened a Realtime connection, returned `PUCK DIRECT REALTIME WORKS`, received `response.done`, and closed normally.
- Two consecutive normal turns completed. Both captured nonempty mic PCM, accepted `input_audio_buffer.append` → `commit` → `response.create`, received responses, drained playback, and reached `final_turn_complete`. The Talk button re-armed after each turn.
- Turn 1 captured 532,480 bytes and produced a 21-character input transcript. Turn 2 captured 506,880 bytes and produced a 254-character input transcript. That longer second transcript suggests extra sound entered the capture, so its spoken-content check is inconclusive even though the transport/playback lifecycle completed.
- Playback logs reported expected frame counts for every chunk. Acoustic quality was not independently judged during this session.
- A short direct `Test speaker` tone also completed its expected 16,000 frames in an earlier device check.

## Current code cleanup

The active tree now removes the Mac relay and fallback transport, the Tower wake client, the old web bridge, old scaffolding, and the older TFLite wake pipeline. The direct Realtime path and local microWakeWord engine remain. Build and install validation is still needed after these removals.

The development build embeds `OPENAI_API_KEY` from local `secrets.properties` in BuildConfig. The intended short-lived session credential handoff is not implemented.

## Next work

1. Build the cleaned Android source and address any compile or packaging issues.
2. Install the cleaned build on the Show and recheck direct Realtime and local wake behavior.
3. Improve controlled speech capture so a test turn contains only the intended phrase.
4. Continue hardware privacy PCM measurements and device-specific button handling.
5. Camera, full-duplex/barge-in, cold-boot recovery, durable authentication, memory/capability runtime and USB host remain separate milestones.

See `docs/WORK_INDEX.md` for provenance. Files under `docs/history/` are archival and may document code that has since been removed.
