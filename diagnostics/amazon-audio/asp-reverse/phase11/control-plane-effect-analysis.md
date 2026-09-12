# Control-plane effect analysis

The HAL stores effect-interface pointers, compares their descriptor UUIDs, and uses them to maintain internal effect lists. `CheckNativeEffect` then synchronizes the configuration-owned pointer list into `AudioPreProcess`. The recovered paths do not show PCM being processed by the Android effect interface itself; Amazon’s internal preprocess/AEC path performs the actual work. This supports a control-plane interpretation, but the complete framework-side instantiation path is not proven.
