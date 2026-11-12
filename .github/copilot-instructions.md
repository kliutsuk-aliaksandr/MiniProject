# Copilot instructions — Marble Frog

Read root `AGENTS.md` and the nearest nested guide before editing.

This is an offline Kotlin Android Canvas marble-shooter. Preserve offline-only behavior and local save compatibility. Work on `dev`; do not modify `main` or rewrite history unless explicitly requested.

Respect architecture: Android-independent `game/domain`, persistence in `game/data`, Canvas in `game/render`, orchestration/input/accessibility in `game/ui`, offline sound in `game/audio`.

Add deterministic JUnit/Robolectric regression tests for behavior changes. Tutorial practice and challenges are isolated from campaign saves/progression. Visible UI strings need Russian and English resources.

Use JDK 21 and Gradle Wrapper. Keep release R8 minification enabled, retain R8 9.1.29 for Kotlin 2.4.21, increment versionName/versionCode, and run minified AAB checks. Never suppress metadata warnings or bypass Sonar Quality Gate to get a green build.

Never commit credentials, upload keys, build output or local machine configuration. Update README/changelog/roadmap/release docs with behavior or workflow changes. Report only checks actually executed.
