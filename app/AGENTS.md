# Android app rules

- Read the repository root `AGENTS.md` first.
- Use JDK 21 for Gradle/Android/Sonar tasks; keep Java/Kotlin bytecode target at 17 unless compatibility work is planned.
- Preserve `minSdk = 23`, `targetSdk = 36` and `compileSdk = 36` unless documented otherwise.
- For a release, increment both `versionName` and `versionCode`; CI's release AAB is intentionally unsigned.
- Keep `isMinifyEnabled = true` in release. Keep the explicit R8 override aligned with Kotlin and official Android R8 requirements.
- Never add secrets, signing keys/passwords or local SDK paths to Gradle files or source control.
- Keep Android APIs out of domain; SharedPreferences access belongs in repositories/data.
- Keep Russian and English string resources in sync.
- Run unit tests, lint and minified release bundle checks for release-related changes.
