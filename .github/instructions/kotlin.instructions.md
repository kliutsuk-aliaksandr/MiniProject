---
applyTo: "app/src/main/java/**/*.kt"
---

# Kotlin source instructions

- Follow root and nearest nested `AGENTS.md`.
- Preserve project layering and visibility boundaries.
- Keep domain logic platform-independent and prefer immutable, validated state.
- Avoid per-frame allocations, I/O, SharedPreferences access and expensive derived-state calculations.
- Keep visible UI text in paired Russian and English Android string resources.
- Preserve accessibility behavior and align virtual node bounds with rendered/tappable controls.
- Add regression tests for behavior changes and preserve save-data compatibility.
