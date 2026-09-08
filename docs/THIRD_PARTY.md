# Third-party provenance

This repository preserves an existing experimental workspace; no blanket project license is assigned by this consolidation.

- Home Assistant Android reference checkout: https://github.com/home-assistant/android at `d3af0e4c91484b69f8a175c86d1514ea2ae2c907`. Native microWakeWord engine/JNI/frontend and Kotlin integration were adapted from this work. Its Apache 2.0 license is retained in `licenses/Home-Assistant-Apache-2.0.txt`. Puck integration and diagnostic changes are local adaptations; the original checkout is retained locally for comparison.
- Native build dependencies (TFLite Micro, KissFFT, FlatBuffers, gemmlowp, ruy) are pinned by archive revision and SHA-256 in `puckd-android/app/src/main/cpp/CMakeLists.txt`; upstream licenses remain in fetched source trees.
- Android TensorFlow Lite 2.14.0 AARs are retained in `app/libs/` because the existing Gradle build references them directly. TensorFlow project: https://github.com/tensorflow/tensorflow .
- `app/src/main/assets/microwakeword/hey_jarvis.json` attributes the microWakeWord model to Kevin Ahrendt. Related upstream: https://github.com/kahrendt/microWakeWord .
- `app/src/main/assets/wakeword/` contains the earlier openWakeWord model pipeline experiments; upstream https://github.com/dscripka/openWakeWord . Do not assume its pretrained models share the code license. The private voice test sample is excluded.
- `tower-reference/` is a local copied Python wake reference package and is excluded from Git. It is not a captured complete Tower deployment.

The bundled wake model hashes are recorded in `ASSET_SHA256SUMS`. Exact original model download revisions/license mapping were not fully captured in the working files and remain a provenance follow-up before packaging or redistribution as a product.
