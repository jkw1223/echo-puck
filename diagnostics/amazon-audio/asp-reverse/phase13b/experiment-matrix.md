# Experiment matrix

| XML | Audio restart method | Device/app state | AudioRecord result |
|---|---|---|---|
| Original | none/current boot | unlocked/foreground | PASS; PCM capture logs present |
| Original | audioserver restart | unlocked/foreground | PASS; PCM capture logs present |
| Original | full reboot | unlocked/foreground | PASS after explicit keyguard dismissal; PCM capture logs present |
| AEC test | audioserver restart | unlocked/foreground | Not repeated in Phase 13B; Gate 1 previously loaded registration, but Gate 2 stopped before effect creation |
| AEC test | full reboot | unlocked/foreground | UNAVAILABLE: temporary remount is lost across reboot, so test XML was not installed persistently |
