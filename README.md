# Echo Puck

Repurposing a first-generation Amazon Echo Show 5 (`checkers`) as Puck 0: a microphone, speaker, touchscreen and eventual camera endpoint for Steve.

Workspace: `~/puck`. Repository: https://github.com/jkw1223/echo-puck.

Start with [CURRENT_STATE.md](CURRENT_STATE.md) for the real stopping point and [the work index](docs/WORK_INDEX.md) for collected history. Direct Realtime audio has been demonstrated; the normal two-turn lifecycle regression remains incomplete.

## Workspace

| Path | Purpose |
|---|---|
| `puckd-android/` | Active Android app, shared capture, transports, playback and wake experiments |
| `puck-relay/` | Mac relay, fixed speech fixture, supervised browser queue, provider adapters and tests |
| `puckd-files/`, `puckd/` | Original scaffolding and isolated probe reference |
| `experiments/web-bridge/` | September 5 Python bridge scaffold |
| `docs/history/` | Original architecture and checkpoints |
| `artifacts/` | Local-only logs, screenshots, recordings, rollback APKs and source snapshots |
| `.local/` | Local-only conversation archives, inventory and verification logs |
| `ha-android-ref/`, `tower-reference/` | Existing local third-party reference material |

## Development

Relay, with Node.js installed:

```sh
cd ~/puck/puck-relay
npm ci
npm test
lsof -nP -iTCP:8787 -sTCP:LISTEN
npm start
```

Inspect the chosen mode and existing listener before starting the relay; it loads local provider configuration from `.env.local`. See [historical protocol documentation](docs/history/README-2026-09-06.md).

Android requires SDK 37, the checked-in Gradle/JDK toolchain, and native CMake/NDK dependencies. Configure the SDK with local `local.properties` or the Android environment:

```sh
cd ~/puck/puckd-android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

`-PrelayUrl=ws://HOST:8787/puck` overrides the historical relay address. Compilation permits an absent `secrets.properties`; direct provider calls require local configuration. The current experiment embeds `OPENAI_API_KEY` from that file in BuildConfig, so APKs are private development artifacts. Keep the existing local debug signing key for in-place updates.

Optional wake diagnostics use a private `wake-sample.wav` retained locally and absent from GitHub. No device install/provider call is part of repository setup. Verify actual device/host state before using historical commands.

See [third-party provenance](docs/THIRD_PARTY.md). Original checkpoints remain under `docs/history/`; the supervised web experiment and later direct API transports are distinct.
