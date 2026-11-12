# Test rules

- Read repository and app-level `AGENTS.md` before changing tests.
- Tests must be deterministic, independent and safe in any order.
- Clear or namespace SharedPreferences before and after each test.
- Use JUnit 4 and Robolectric for Android Context/resources; pure domain tests should not require Robolectric.
- Prefer observable behavior and invariants: mode isolation, save compatibility, progress, input, countdown, accessibility and restart.
- For regression tests, assert expected state and negative side effects (for example, tutorial restart does not create a campaign save).
- Keep device-only checks in `app/src/androidTest`; JVM tests cannot prove TalkBack/device behavior.
- Generate coverage with `:app:jacocoDebugUnitTestReport`; do not remove meaningful production classes from JaCoCo scope.
