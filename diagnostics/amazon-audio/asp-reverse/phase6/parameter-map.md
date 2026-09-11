# Parameter map

Confirmed `AudioParameter` key objects in the device-level handler are `routing`, `sampling_rate`, `format`, `channels`, `frame_count`, `input_source`, and `screen_state`. The handler calls `getFloat`, `getInt`, and `remove`. No exact literal key comparison for AEC, echo, VoIP, SPE, ASP, or force-AEC was proven. No speculative parameter was sent.
