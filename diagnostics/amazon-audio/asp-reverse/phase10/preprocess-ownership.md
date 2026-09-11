# Preprocess ownership

`AudioPreProcess` constructor: `0x8ac14`, taking `stream_attribute_t const*`. Its destructor and cleanup paths are present in the HAL. The constructor initializes the preprocess object and its effect list; the retained static evidence does not identify a direct `AudioALSAStreamIn` field pointing to this object. The stream constructor instead stores the stream manager at `+0x04` and speech-enhancement singleton at `+0x160`. Ownership is therefore unresolved between the stream manager/capture client and preprocess object.
