# CheckNativeEffect callers

`CheckNativeEffect` is at `0x8b050`. The retained XREF output did not produce a complete caller list in this pass, so the event that invokes synchronization is UNKNOWN. The function itself is clearly designed to reconcile a configuration-owned effect array with the preprocess object, which strongly suggests a capture/preprocess lifecycle or configuration event rather than the stream `+0x48` callback.
