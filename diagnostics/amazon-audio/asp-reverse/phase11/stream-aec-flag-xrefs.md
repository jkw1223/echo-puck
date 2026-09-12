# Stream AEC flag XREFs

The recognized-AEC branch in `AudioALSAStreamIn::addAudioEffect` writes `this+0x114 = 1`; the matching remove branch writes `this+0x114 = 0`. The current static inventory did not recover a complete set of reads of `this+0x114`. The constructor initializes the surrounding object but does not assign a semantic AEC value there. Status: writes PROVEN; readers and translation into capture state UNKNOWN.
