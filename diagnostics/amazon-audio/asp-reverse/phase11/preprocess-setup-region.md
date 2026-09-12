# Preprocess setup region

All four confirmed calls are inside one function: `android::AudioPreProcess::CheckNativeEffect()` at `0x8b050`, ending at `0x8b320`.

- `0x8b0dc`: remove stale effect while synchronizing current list.
- `0x8b160`: remove a current effect absent from configuration.
- `0x8b258`: add a configured effect absent from the current list.
- `0x8b2e0`: add configured effects when the current list is empty/short.

Calling convention is ARM `this` in `r0`; `CheckNativeEffect()` takes only `this`. Its direct callees are `AudioPreProcess::addAudioEffect` and `removeAudioEffect`.
