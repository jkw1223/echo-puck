# Runtime effect descriptors

A direct `AudioEffect.queryEffects()` descriptor list was not collected because the Show was locked and the installed diagnostic app’s query runs from its capture-start path. The running AudioFlinger dump is still decisive for loaded libraries: only bundle, reverb, visualizer, downmix, loudness enhancer, and dynamics processing are loaded. No `pre_processing`, AEC, or NS library appears.

The observed framework state remains `AcousticEchoCanceler.isAvailable() == false`.
