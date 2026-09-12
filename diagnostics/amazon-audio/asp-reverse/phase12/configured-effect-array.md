# Configured effect array

Fields proven by ARM decompilation:

```text
config + 0xdc  dirty/native-effect synchronization flag
config + 0xe0  configured effect count
config + 0xe8  effect-interface pointer array, indexed by 4 bytes
```

`AudioPreProcess::CheckNativeEffect` compares that array against its current list at `this+8 + index*0xc`. The configuration array is read by the synchronizer; its writers were not fully recovered in this pass.
