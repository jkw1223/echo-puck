# Preprocess lifetime

`AudioPreProcess` is constructed at `0x8ac14` from `stream_attribute_t const*` and destroyed by the destructor beginning at `0x8adcc`. The object owns its current effect list and echo-reference pointer (`+0x48`). The retained code does not prove whether instances are global, per capture stream, per handler, or per session; external allocator/owner XREFs remain to be recovered.
