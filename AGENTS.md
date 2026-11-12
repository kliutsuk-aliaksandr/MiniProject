# Agent guide — Marble Frog

Read this repository-wide guide before changing code, then read the nearest nested `AGENTS.md`.

## Project invariants
- Offline Kotlin Android marble-shooter. Preserve the no-account/no-ads/no-analytics/no-INTERNET design unless the product owner approves a privacy and architecture change.
- Saved-game and progress formats are user data. Preserve backward compatibility and schema migrations; never silently reset existing saves.
- Tutorial/practice and challenges must not mutate campaign saves, stars, unlocks or lifetime campaign statistics unless explicitly designed and tested.
- Changes to collisions, chain movement, insertions, cascades, routes and booster inventory require focused regression tests.
- Do not remove release R8 minification or weaken CI checks to manufacture a green build.

## Branch and Git policy
- Develop and commit application changes on `dev`; `main` is the stable/release line.
- Do not push directly to `main`, merge branches, create releases/tags, or change GitHub Rulesets unless explicitly asked.
- Never force-push, rewrite shared history or discard user changes. Use expected-head/lease checks.
- Keep commits scoped and descriptive. Never commit generated output, credentials, keystores or local IDE state.

## Build and verification
- Use the committed Gradle Wrapper and JDK 21 for Gradle/Android/Sonar tooling. App bytecode target remains Java/Kotlin 17.
- Android compile/target SDK is 36. Do not change minSdk/targetSdk without reviewing release impact.
- Run relevant checks as appropriate: `./gradlew testDebugUnitTest`, `./gradlew :app:jacocoDebugUnitTestReport`, `./gradlew lintDebug`, `./gradlew assembleDebug`, `./gradlew bundleRelease`, `./gradlew sonar`.
- A release change is not verified until minified `bundleRelease` completes without Kotlin metadata warnings. CI enforces this.
- SonarQube must import JaCoCo XML and pass the server-side Quality Gate on trusted push/same-repository PR builds.
- In summaries, distinguish source inspection, locally executed commands and actual GitHub Actions results. Do not claim tests passed without evidence.

## Architecture boundaries
- `game/domain`: Android-independent simulation, rules, models, catalog and events; no Android APIs.
- `game/data`: local persistence and progress repositories; preserve schema, validation and migration behavior.
- `game/render`: Canvas drawing and presentation state; no persistence/gameplay rules.
- `game/ui`: View adapter, input, navigation, session orchestration and accessibility.
- `game/audio`: offline sound adapter.
- Prefer small cohesive changes over broad mechanical rewrites.

## Kotlin and Android quality
- Follow Kotlin style; prefer immutable values, explicit transitions and narrow visibility.
- Avoid allocations, SharedPreferences reads and expensive aggregation in the per-frame render path.
- Respect lifecycle, stop animation when inactive, release audio, and avoid unnecessary keep-screen-on behavior.
- Keep touch geometry, rendered controls and accessibility bounds aligned.
- Do not add broad ProGuard keep rules to hide R8 metadata warnings. R8 9.1.29 is explicitly pinned for Kotlin 2.4.21; update deliberately with official compatibility evidence.
- Update versionName and increment versionCode together. Current target: 0.5.1 / versionCode 6.

## Tests and docs
- Add regression tests for confirmed bugs, including negative side effects and isolation from unrelated modes/data.
- Use JUnit/Robolectric for local behavior; instrumentation for Android framework, accessibility and device-specific checks.
- Clear/isolate SharedPreferences in tests; do not rely on execution order or developer data.
- Update README, changelog, roadmap, release checklist and technical docs when behavior/tooling/release procedure changes.
- Do not mark physical-device QA, Play Console setup, signing, or remote Sonar Quality Gate complete without evidence.
