# Echo-reference creation

`start_echo_reference` at `0x8ba50` first clears the prior interface, stores the requested values at `+0x50` and `+0x54`, and calls `create_echo_reference(1, param_3, param_4, 1, 2, param_4, this+0x48)`. The producer dimensions therefore come from the preprocess configuration object at `this+0x5c` offsets `+0x18` and `+0x1c`. The call is reached from `AudioPreProcess::addAudioEffect` at `0x8b960`; another caller is at `0x8c0b4`.
