# Game domain rules

- Read the repository and app-level `AGENTS.md` files first.
- Keep this package pure Kotlin and Android-independent: no `Context`, `View`, `Canvas`, `SharedPreferences`, resources or framework classes.
- Model gameplay as explicit state transitions and domain events; keep simulation testable on the local JVM.
- Preserve level IDs, track semantics, ball spacing/order, collision insertion direction, cascade timing, win/loss rules and booster inventory semantics.
- Validate restored values and preserve old save compatibility. Migrations must be deliberate and tested.
- Tutorial and challenge flags remain distinct from campaign state. Changes to one mode must not contaminate another mode's progress.
- Prefer seeded randomness in tests; cover boundaries as well as typical cases.
- Add focused tests for collisions, insertion, retraction/cascades, routes, save/restore and boosters.
