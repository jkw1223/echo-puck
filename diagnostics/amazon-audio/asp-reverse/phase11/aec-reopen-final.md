# AEC reopen branch

The diagnostic `AECOn, need reopen the capture handle` remains associated with a capture configuration path, but the exact containing function and old/new state comparison were not recovered in this pass. `CheckNativeEffect` reconciles preprocess effects, but it contains no capture close/open sequence. The condition and handle fields remain UNKNOWN.
