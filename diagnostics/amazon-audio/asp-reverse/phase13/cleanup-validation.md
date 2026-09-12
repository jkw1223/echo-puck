# Cleanup validation

The original `/system/vendor/etc/audio_effects.xml` was restored, permissions/owner were reset to `0644 root:root`, and `restorecon` was run. The restored SHA-256 is `61c6fe7cdd977c32b0014bdf1b07277b1cb7bebe1369b8907660fdee72d639a3`, matching Gate 0. Audioserver was restarted. The restored AudioFlinger dump reports the original library set and no `pre_processing` library. No AEC object was created, so no effect-release cleanup was required.
