# Enable-call boundary

The preserved Phase 14 log does not support the original ordering claim. `AecDiag event=016` (`create_aec`) is timestamped 22:06:42.898. The matching native tombstone is timestamped 22:06:45.689, while the harness logs `AecDiag event=017 STEP=enable_aec` only at 22:06:58.698. Therefore the native crash predates the enable request by approximately 13.0 seconds.

The tombstone's stack is entirely HIDL audio-service code: `Device::debug` -> `BnHwBase::_hidl_debug` -> `BnHwDevice::onTransact` -> HIDL binder pool. It contains no `AudioEffect`, `libaudiopreprocessing`, AudioFlinger effect, Amazon HAL, `AudioPreProcess`, or ASP frame.

Conclusion: the captured tombstone cannot establish an AEC enable call boundary. The deepest proven AEC boundary remains Android effect creation (`AcousticEchoCanceler.create`); enable was logged after the service had already crashed and returned `-7` because the binder service was dead.
