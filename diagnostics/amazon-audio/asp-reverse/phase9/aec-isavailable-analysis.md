# `AcousticEchoCanceler.isAvailable()`

The observed result is `false`. Android availability depends on framework effect-factory registration of a descriptor with the AEC type UUID; the HAL's internal UUID comparison alone is insufficient. Because no installed effect configuration or factory runtime snapshot is retained, the exact failure cause is UNKNOWN. The evidence supports “registration/factory visibility is missing or unavailable,” but does not distinguish absent XML, absent library, or rejected descriptor.
