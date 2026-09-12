# Effect control-plane classification

The Amazon HAL obtains effect descriptors, compares UUIDs, stores interface pointers, and synchronizes configured interfaces into `AudioPreProcess`. The captured internal paths do not show Android effect PCM-processing callbacks being used by the Amazon AEC path. The actual processing is performed by `AudioPreProcess`/ASP/SPE. Classification: the framework effect object is a CONTROL-PLANE TRIGGER for this HAL path, pending a complete framework-to-config pointer lineage proof.
