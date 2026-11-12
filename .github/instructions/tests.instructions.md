---
applyTo: "app/src/test/**/*.kt,app/src/androidTest/**/*.kt"
---

# Kotlin test instructions

- Follow root and nearest nested `AGENTS.md`.
- Use deterministic inputs and isolate SharedPreferences; do not depend on test order.
- Prefer pure JUnit for domain logic and Robolectric for UI/session code requiring Android resources.
- Assert mode isolation, especially for tutorial and challenge flows.
- Do not claim Android instrumentation behavior from local JVM tests.
- Do not remove meaningful production code from JaCoCo scope to inflate coverage.
