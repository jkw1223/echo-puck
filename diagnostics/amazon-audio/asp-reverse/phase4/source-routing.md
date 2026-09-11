# Source routing

The HAL contains explicit diagnostic format strings for `input_source` alongside `mAudioMode`, input device, sample rate, channel count, `bVoIPEnable`, and `bypassDualProcess`. It also contains `AECOn` capture-handle lifecycle text. This proves source and mode participate in capture configuration, but the current disassembly does not establish that source values 1, 6, 7, or 9 independently call `EnableNormalModeVoIP`, set `mForceAECRec`, or create an echo reference.

The controlled VR/VC test therefore remains correctly interpreted: changing source 6 to 7 alone did not materially change the signal.
