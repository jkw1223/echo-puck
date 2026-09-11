# Device parameter handling

No exact vendor key/value was promoted as confirmed. The binary contains generic Android `routing`, mode, and stream-manager strings plus internal labels such as `input_source`, `AECOn`, and `SetForceAECRec`, but strings alone do not prove that an external `str_parms` key is accepted by `adev_set_parameters`.

The safe conclusion is that a parameter candidate must not be guessed from these labels. ARM-aware recovery of the device function table and `str_parms` call sites is still required.
