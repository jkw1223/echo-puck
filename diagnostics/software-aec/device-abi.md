# Echo Show ABI

Collected: 2026-09-12 (America/Chicago)

Device endpoint: `192.168.1.24:5555`

```text
ro.product.model       Echo Show 5
ro.product.device      checkers
ro.build.display.id    lineage_checkers-userdebug 11 RQ3A.211001.001 eng.r0rt1z.20260905.021248 test-keys
ro.build.version.release 11
ro.product.cpu.abi     armeabi-v7a
ro.product.cpu.abilist  armeabi-v7a,armeabi
uname -m                armv8l
id                      uid=0(root) gid=0(root) groups=0(root),1004(input),1007(log),1011(adb),1015(sdcard_rw),1028(sdcard_r),1078(ext_data_rw),1079(ext_obb_rw),3001(net_bt_admin),3002(net_bt),3003(net_bw_stats),3009(readproc),3011(uhid) context=u:r:su:s0
```

The build target is therefore 32-bit ARM (`target_cpu="arm"`), not ARM64. No device configuration was changed.
