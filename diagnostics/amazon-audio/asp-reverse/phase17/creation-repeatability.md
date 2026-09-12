# Creation repeatability

Across the preserved runs:

```text
Phase 14: create succeeded (session 25)
Phase 16: create returned null (session 9)
Phase 17: create succeeded (session 41)
```

This is not deterministic failure. The available evidence supports intermittent/resource-state or lifecycle dependence. Only one Phase 17 creation attempt was made after instrumentation, and it succeeded. No enablement was attempted.
