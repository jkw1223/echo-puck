# Deferred runtime test

No device, APK, mixer, vendor HAL, or proprietary ASP call was executed or modified. Runtime activation remains explicitly deferred until the static bridge from framework effect creation to `AudioPreProcess::addAudioEffect` (or an equivalent proven caller) and the `AECOn` reopen branch are recovered.
