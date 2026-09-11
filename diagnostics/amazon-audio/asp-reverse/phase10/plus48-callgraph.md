# `+0x48` call graph

`AudioALSAStreamIn::addAudioEffect` invokes `(*(this->vtable+0x48))(this)` only when its stream field at `+0x28` is null and the descriptor UUID matches AEC. The target is `AudioALSAStreamIn::set`, whose normal signature has stream-format/configuration arguments; the effect callback invocation supplies only `this`, so this is a virtual lifecycle/state callback through a decompiler-imperfect prototype, not a proven call carrying the effect pointer.

The complete `set` body performs stream configuration, calls virtual entries `+0x50`, `+0x54`, and `+0x58`, computes buffer sizing, and stores configuration fields. It contains no call to `AudioPreProcess::addAudioEffect`, `removeAudioEffect`, `start_echo_reference`, or `SetForceAECRec`. The callback therefore does not close the missing edge.
