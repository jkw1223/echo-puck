# Phase 14 versus Phase 16/17

| Run | Session | Create path | Result | Service | Notes |
|---|---:|---|---|---|---|
| Phase 14 | 25 | `AcousticEchoCanceler.create` | non-null, control=true | died only after an intervening `dumpsys` | AEC object existed before service death |
| Phase 16 | 9 | `AcousticEchoCanceler.create` | null | stable for 15 seconds | no dumpsys; service remained alive |
| Phase 17 | 41 | `AcousticEchoCanceler.create` | non-null, disabled, control=true | stable | no enable; same overlay and format |

The evidence shows creation is resource/lifecycle/state dependent or intermittent, rather than a deterministic descriptor-registration failure. The difference is not explained by the audio format or source, which were held constant. Phase 17 also shows that a generic `AudioEffect` request with the same AEC type returns native error `-3`, while the AEC helper's implementation-specific static path can succeed.
