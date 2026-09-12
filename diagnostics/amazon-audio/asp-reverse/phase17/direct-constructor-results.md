# Direct constructor results

The harness attempted reflective lookup of `AcousticEchoCanceler.<init>(int)` after releasing the successful static AEC object. The framework class exposes no such constructor to reflection:

```text
java.lang.NoSuchMethodException: android.media.audiofx.AcousticEchoCanceler.<init> [int]
```

The public static `AcousticEchoCanceler.create(session)` path did succeed in the same fresh session (`session=41`, non-null, initially disabled, `hasControl=true`). No enable call was made.
