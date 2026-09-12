# Phase 13C report

## Gate 0

The Show was verified unlocked/foreground before the control. Original XML hash and metadata were preserved. Ordinary `AudioRecord` produced nonzero PCM with the production XML.

## Overlay and Gate 1

A temporary bind mount from `/data/local/tmp/audio_effects-test.xml` over `/system/vendor/etc/audio_effects.xml` succeeded. The visible test hash was `1e25578f8df14bd5f46169e5696f28d29b76286c9c1700fd4a6e5e03df2b4b16`.

After audioserver restart, AudioFlinger loaded:

```text
Library pre_processing
Acoustic Echo Canceler
UUID: bb392ec0-8d4d-11e0-a896-0002a5d5c51b
TYPE: 7b491460-8d4d-11e0-bd61-0002a5d5c51b
```

Ordinary unlocked foreground `AudioRecord` continued to produce PCM. This proves registration does not by itself prevent ordinary capture.

## Gate 2 stop

Gate 2A was not run. The installed production diagnostic APK has no safe control for creating an `AcousticEchoCanceler` object; its current capture path logs PCM but does not expose the required create-only/enable sequencing. I did not modify or replace the production APK to add such a control. Therefore no AEC object was created, no `setEnabled(true)` call was made, and no Amazon-side activation claim is possible.

The requested Gate 2 artifacts are marked not run. No capture-reopen, echo-reference, or remove-effect runtime evidence was collected.

## Rollback

The bind mount was explicitly unmounted, the staged file was removed, audioserver was restarted, and the original XML hash was restored:

```text
61c6fe7cdd977c32b0014bdf1b07277b1cb7bebe1369b8907660fdee72d639a3
```

AudioFlinger no longer reports `pre_processing`. No HAL, ASP, mixer, or production APK state was changed.

## Answers

1. Bind-mount overlay: **yes**.
2. AEC registration remained visible: **yes**.
3. Direct `AcousticEchoCanceler.isAvailable()` call: **not collected**; AudioFlinger exposed the exact descriptor.
4. Ordinary AudioRecord with registration: **yes**.
5. AEC creation: **not attempted**.
6–12. AEC activation, enable timing, attachment order, reopen, echo reference, capture health with AEC, and release reversal: **not run**.
13. Amazon internal AEC runtime path: **unproven**.
14. Gate 3 leakage testing: **not justified**.
