# SonarQube and code coverage

The Android debug variant generates JaCoCo coverage reports for both local JVM tests and Android instrumentation tests.

## Local analysis

Use JDK 21 or later for the SonarScanner process. The app still compiles to JVM 17.

```bash
export SONAR_TOKEN="your-project-token"
export SONAR_HOST_URL="https://your-sonarqube-host"
export SONAR_PROJECT_KEY="your-existing-project-key"
./gradlew testDebugUnitTest
./gradlew :app:jacocoDebugUnitTestReport
./gradlew sonar
```

If the project is hosted on SonarQube Cloud, set `SONAR_HOST_URL=https://sonarcloud.io`. The project key must match the already-created SonarQube project. The Gradle build defaults to `kliutsuk-aliaksandr_MiniProject`; override it when your existing key differs.

## GitHub Actions configuration

In **Repository Settings → Secrets and variables → Actions**, add:

- Repository secret `SONAR_TOKEN`: a project analysis token with permission to submit analysis.
- Repository variable `SONAR_HOST_URL`: the URL of the connected SonarQube Server or Cloud instance (optional; default is SonarQube Cloud).
- Repository variable `SONAR_PROJECT_KEY`: the exact key of the existing project (optional; default is `kliutsuk-aliaksandr_MiniProject`).

The Android CI always generates and uploads the unit-test coverage report. When `SONAR_TOKEN` is present, the workflow switches to JDK 21 and runs the `sonar` task. Without that secret, only the scanner step is skipped; build, tests, lint and the coverage artifact still run.

The scanner imports JaCoCo XML, JUnit XML and Android Lint results. The `prepareSonarCoverageReport` task discovers the AGP-generated XML report and copies it to a stable root build path used by Sonar.

## Coverage scope

The first Sonar gate imports JVM/Robolectric test coverage. Android instrumentation tests also have AGP coverage enabled; their API 23/29/36 jobs remain independently validated and retain their own test reports. Merging device-specific execution data into one Sonar coverage report is intentionally not claimed yet.

## Useful Gradle tasks

- `./gradlew :app:jacocoDebugUnitTestReport` — JVM/Robolectric JaCoCo XML/HTML coverage.
- `./gradlew :app:createDebugAndroidTestCoverageReport` — connected-device coverage (requires an available emulator or device).
- `./gradlew sonar` — import coverage, JUnit and lint data and submit the analysis.
