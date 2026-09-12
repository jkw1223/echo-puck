# Phase 13B report

The Gate 2 failure was caused by device/app state, not by a proven AEC registration incompatibility.

With the original XML, `AudioRecord` succeeds when the Show is actually unlocked and the diagnostic app is foreground. The earlier failed run occurred while the device was still in the dreaming/keyguard state; AudioPolicy logged:

```text
getInputForAttr permission denied: recording not allowed for uid 10158
```

The package has `RECORD_AUDIO` granted and AppOps reports `RECORD_AUDIO: allow`, so the denial is policy/state related rather than a missing manifest permission or denied AppOp.

The original XML control also succeeds after an audioserver-only restart when the device is unlocked, and after a full reboot once keyguard is explicitly dismissed. Therefore the earlier status `-1`/Java `-20` was not reproduced under the correct foreground/unlocked control.

The attempted AEC-test full-reboot cell was unavailable: the temporary writable remount is lost during reboot, and the test XML was not active after boot. The original XML remained installed throughout that comparison. No AEC object was created and no effect was enabled.

## Answers

1. Ordinary AudioRecord works under original XML: **yes, unlocked/foreground**.
2. Isolated audioserver restart causes failure with original XML: **no, not when unlocked/foreground**.
3. Status `-1` tied to permission/AppOps: **policy denial while keyguard/dreaming; AppOps and manifest permission are allowed**.
4. Lock/background state matters: **yes; the failed run was locked/dreaming**.
5. Full reboot restores capture: **yes after explicit unlock**.
6. AEC registration after full reboot: **not tested because the temporary remount did not survive reboot**.
7. AudioRecord with AEC registered after clean reboot: **not tested**.
8. Registration itself breaks capture: **not supported by available controls**.
9. Resume procedure: keep the device unlocked/foreground, use a persistent reversible overlay mechanism if needed, verify ordinary AudioRecord first, then repeat Gate 1 and only afterward Gate 2.
