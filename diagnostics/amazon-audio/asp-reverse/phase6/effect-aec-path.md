# Effect/AEC path

`AudioPreProcess::addAudioEffect` contains a descriptor comparison before a call to `start_echo_reference`. The comparison is proven to exist, but the descriptor bytes and UUID are not recovered. Therefore the effect cannot be classified as standard Acoustic Echo Canceler, another standard preprocessing effect, or vendor-specific. This edge remains **UNKNOWN**.
