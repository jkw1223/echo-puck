# Stream AEC state

The AEC recognition branch writes stream byte/field `+0x114 = 1`; the symmetric remove branch clears it. The stream constructor initializes nearby state and stores `AudioSpeechEnhanceInfo*` at `+0x160`. The `+0x48` field used as the callback gate is also populated by `set` with a channel-count-derived value. No complete reader/writer XREF inventory proving that `+0x114` drives preprocess or capture reopen was recovered. Status: the write is PROVEN; downstream semantic use is UNKNOWN.
