# Проверка качества кода и покрытие тестами

## Локальные команды

Для сборки и основных проверок нужны JDK 21 и установленный Android SDK.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew :app:jacocoDebugUnitTestReport
./gradlew lintDebug
./gradlew bundleRelease
```

## JaCoCo coverage

Задача `jacocoDebugUnitTestReport` запускает JVM/Robolectric unit tests и формирует:

- XML: `app/build/reports/jacoco/debugUnitTest/report.xml`
- HTML: `app/build/reports/jacoco/debugUnitTest/html/index.html`

В Android CI эти файлы загружаются artifact `miniproject-unit-test-coverage`. Отчёт отражает покрытие JVM/Robolectric-тестами; покрытие connected instrumentation tests не смешивается с ним.

Для генерации отдельного отчёта connected Android tests требуется доступный эмулятор или устройство:

```bash
./gradlew :app:createDebugAndroidTestCoverageReport
```

## GitHub Actions

Workflow проверяет:

- debug APK и JVM/Robolectric unit tests;
- JaCoCo XML/HTML report;
- Android Lint;
- unsigned release AAB и отсутствие известных Kotlin metadata warnings от R8;
- UI instrumentation на Android API 23, 29 и 36;
- Gradle Wrapper на Windows.

CI сохраняет debug APK, unsigned release AAB, JaCoCo coverage и instrumentation test reports как отдельные artifacts. Облачный анализатор кода не требуется; отчёт покрытия доступен непосредственно из GitHub Actions.

Unsigned AAB предназначен для проверки сборки и не подходит для прямой загрузки в Play Console без подписи.
