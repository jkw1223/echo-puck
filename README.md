# Echo Puck

Repurposing a first-generation Amazon Echo Show 5 (`checkers`) as Puck 0: a microphone, speaker, touchscreen and eventual camera endpoint for Steve.

Workspace: `~/puck`. Repository: https://github.com/jkw1223/echo-puck.

Start with [CURRENT_STATE.md](CURRENT_STATE.md) for the latest device evidence and [the work index](docs/WORK_INDEX.md) for project history. The active Echo Show voice path uses direct OpenAI Realtime audio. The retired Tower and Mac audio-routing implementations have been removed from the active source tree; their decision history remains in `docs/history/`.

The Show handles microphone capture, local wake detection, UI, the direct Realtime WebSocket and response playback. OpenAI provides model inference and response audio. The intended off-device role is long-lived credential provisioning; the current development APK still embeds its locally supplied key, so short-lived session credentials remain to be implemented. See [the current Steve brief](STEVE_BRIEF.md).

## Workspace

| Path | Purpose |
|---|---|
| `puckd-android/` | Android app, shared capture, direct Realtime audio, playback and local wake detection |
| `diagnostics/` | Audio and software AEC investigations |
| `docs/history/` | Original architecture decisions and checkpoints |

## Development

Android requires SDK 37, the checked-in Gradle/JDK toolchain, and native CMake/NDK dependencies. Configure the SDK with local `local.properties` or the Android environment:

```sh
cd ~/puck/puckd-android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Compilation permits an absent `secrets.properties`; direct provider calls require local configuration. The development build embeds `OPENAI_API_KEY` from that file in BuildConfig, so APKs are private development artifacts. Keep the existing local debug signing key for in-place updates.

No device install or provider call is part of repository setup. Verify actual device and host state before using historical commands.

See [third-party provenance](docs/THIRD_PARTY.md). Historical checkpoints remain under `docs/history/`; their relay and Tower setup instructions describe retired code.
