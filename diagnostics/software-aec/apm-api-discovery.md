# Pinned APM API discovery

Inspected 2026-09-12 using official Gitiles headers at WebRTC
`d9ec82c89d94c319e2a1f547c1bdd58591828455`. Build verification remains pending.

Sources: `api/audio/audio_processing.h`,
`api/audio/builtin_audio_processing_builder.h`,
`api/audio/audio_processing_statistics.h`. The legacy
`modules/audio_processing/include/audio_processing.h` forwards to the API header.

## Construction and configuration

`webrtc::AudioProcessing` derives from `RefCountInterface`.
`BuiltinAudioProcessingBuilder` supports a default constructor, a constructor
accepting `const AudioProcessing::Config&`, and `SetConfig(const Config&)`
returning a builder reference. Its `Build(const Environment& env)` returns a
nullable `webrtc::scoped_refptr<AudioProcessing>`; it is not an old no-argument
`Create()` API. `api/environment/environment_factory.h` supplies `webrtc::CreateEnvironment()`
returning an `Environment`; this was also verified in the pinned local source.
Its GN dependency is `//api/environment:environment_factory`.

`ApplyConfig(const Config&)` returns `void`; `GetConfig() const` returns `Config`.
`Initialize()` and `Initialize(const ProcessingConfig&)` return integer status.

The initial configuration must explicitly use:

```cpp
webrtc::AudioProcessing::Config config;
config.echo_canceller.enabled = true;
config.echo_canceller.enforce_high_pass_filtering = false;
config.noise_suppression.enabled = false;
config.high_pass_filter.enabled = false;
config.gain_controller1.enabled = false;
config.gain_controller2.enabled = false;
```

AEC's `enforce_high_pass_filtering` defaults to true, so turning off only
`high_pass_filter.enabled` is insufficient to express the requested HPF-off
configuration. `echo_canceller.export_linear_aec_output` defaults to false.
NS also has `level` (kLow/kModerate/kHigh/kVeryHigh) and
`analyze_linear_aec_output_when_available`. HPF has `apply_in_full_band`.
AGC1 has `mode` (kAdaptiveAnalog/kAdaptiveDigital/kFixedDigital),
`target_level_dbfs`, `compression_gain_db`, `enable_limiter`, and nested
`analog_gain_controller`. AGC2 has nested `input_volume_controller`,
`adaptive_digital`, and `fixed_digital`. Both top-level gain controllers remain
explicitly disabled.

## Processing

The exact PCM16 overloads are:

```cpp
int ProcessStream(const int16_t* const src,
                  const StreamConfig& input_config,
                  const StreamConfig& output_config,
                  int16_t* const dest);
int ProcessReverseStream(const int16_t* const src,
                         const StreamConfig& input_config,
                         const StreamConfig& output_config,
                         int16_t* const dest);
```

These are virtual methods. Both accept approximately 10 ms interleaved PCM16
frames and permit in-place processing. PCM16 initialization requires native
rates with matching input, output, and reverse rates, and matching capture
input/output configuration. Initially all streams are 16000 Hz mono, exactly
160 samples per call. The wrapper must validate this format and buffer length.
The API also supplies deinterleaved float overloads; those are outside scope.

Render processing requires an output buffer even when the C wrapper exposes
only a const render input, so a wrapper-owned scratch frame is required.
The header requires `set_stream_delay_ms(int)` when echo processing is enabled;
its return value is an integer status. A later real-time integration must
supply measured buffering delay, not silently assume hardware delay is zero.
All wrapper operations should initially be serialized by the caller, including
configuration and destruction; destruction must not race processing.

## Statistics

`GetStatistics()` returns `AudioProcessingStats` by value. The overload
`GetStatistics(bool has_remote_tracks)` is deprecated. Stats fields are optional:
`echo_return_loss`, `echo_return_loss_enhancement`,
`divergent_filter_fraction`, `delay_median_ms`, `delay_standard_deviation_ms`,
`residual_echo_likelihood`, `residual_echo_likelihood_recent_max`, and `delay_ms`.
`voice_detected` is deprecated. Missing values must not be reported as zero.
No stats ABI or processing-success claim is made at this stage.
