# HAL entry points

The primary binary is a 32-bit ARM EABI shared object. The exported C++ symbols provide reliable anchors for the internal mode and preprocessing functions. The Android `audio_hw_device`/stream function-table assignments are not fully recoverable from this stripped vendor build with the available static tools, so unnamed `adev_*` and `in_*` addresses are not promoted to confirmed entry points.

| Logical function | Address | Confidence | Evidence |
|---|---:|---|---|
| `AudioALSAHardware::setMode(int)` | `0x0008d6f8` | high | exported symbol and ARM disassembly |
| `AudioALSAStreamManager::setMode(audio_mode_t)` | `0x0005b6a4` | high | exported symbol and call from hardware `setMode` |
| `AudioSpeechEnhanceInfo::SetForceAECRec(bool)` | `0x0007d958` | high | exported symbol |
| `AudioSpeechEnhanceInfo::GetForceAECRecState()` | `0x0007d968` | high | exported symbol |
| `SPELayer::EnableNormalModeVoIP(bool)` | `0x00085e50` | high | exported symbol |
| `SPELayer::WriteReferenceBuffer(InBufferInfo*)` | `0x00084a90` | high | exported symbol |
| `SPELayer::Process_VoIP(short*, int)` | `0x00089240` | high | exported symbol |
| `AudioPreProcess::start_echo_reference(audio_format_t,uint,uint)` | `0x0008ba50` | high | exported symbol; calls `create_echo_reference` |
| `AudioPreProcess::push_echo_reference(uint)` | `0x0008c23c` | high | exported symbol |
| `AudioPreProcess::update_echo_reference(uint)` | `0x0008c52c` | high | exported symbol |
| `AudioPreProcess::stop_echo_reference(...)` | `0x0008afb8` | high | exported symbol |

The full disassembly slices are in this directory. No runtime entry point was invoked.
