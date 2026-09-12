# Preprocess owner

Object type is proven by symbols and constructor use: `AudioPreProcess`, constructor `0x8ac14`, destructor `0x8adcc`, vtable/typeinfo references in the same region. `CheckNativeEffect` uses `this+0x5c` as a configuration/attribute object and `this+8 + index*0xc` as the current effect records. The owner of the `AudioPreProcess` instance outside its own lifecycle is not proven by the retained static data. Confidence: object type HIGH; external owner UNKNOWN.
