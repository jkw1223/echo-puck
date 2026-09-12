# Standalone v2.1 API notes

Source pin: `846fe90a289f58b7c9303a635142aa2c7caa93e5` (WebRTC M131).
Inspected `webrtc/api/audio/audio_processing.h`; the legacy modules header
forwards there. These notes are separate from the September upstream API notes.

Construction uses `webrtc::AudioProcessingBuilder().Create()` returning
`rtc::scoped_refptr<webrtc::AudioProcessing>`. The builder supports
`SetConfig(const AudioProcessing::Config&)`. There is no requirement to supply
the September API's Environment to this builder.

For the later AEC-only experiment, configure:

```cpp
webrtc::AudioProcessing::Config config;
config.echo_canceller.enabled = true;
config.echo_canceller.mobile_mode = false;  // Select AEC3 rather than AECM.
config.echo_canceller.enforce_high_pass_filtering = false;
config.high_pass_filter.enabled = false;
config.noise_suppression.enabled = false;
config.gain_controller1.enabled = false;
config.gain_controller2.enabled = false;
auto apm = webrtc::AudioProcessingBuilder().SetConfig(config).Create();
```

This is a source-verified configuration example, not an executed DSP test.

`ProcessStream` and `ProcessReverseStream` each have a PCM16 overload returning
`int`, accepting `const int16_t* src`, input/output `const StreamConfig&`, and
`int16_t* dest`. Use 16000 Hz, mono, 160 samples per 10 ms call initially.
Validate buffers and lengths in a future C wrapper. Render processing also
requires a destination, so use a scratch frame if the caller's render input is
const. `set_stream_delay_ms(int)` returns status and provides buffering delay;
real timing must be measured during the later device stage.

`ApplyConfig(const Config&)` returns void. `GetStatistics()` returns
`AudioProcessingStats`; the bool-argument overload is deprecated. Stats have
optional values, which must not be converted to false zero measurements.

The standalone library exports C++ API symbols. A stable `SteveApm` C wrapper,
JNI packaging, initial configuration validation, and runtime/device testing
remain later work. Building the upstream native example proves API linkage,
not AEC convergence or acoustic quality.
