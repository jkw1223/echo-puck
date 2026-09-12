# Rollback validation

The original XML remained installed after the failed test-XML reboot attempt. The active hash was checked after boot and the restored AudioFlinger state was collected. No production APK, HAL, mixer, or ASP state was modified. The test XML was never active across a reboot.
