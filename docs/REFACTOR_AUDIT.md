# Полный аудит рефакторинга — Marble Frog

**Дата аудита:** 11 ноября 2026 года
**Проверенная ветка:** fix
**Кодовый snapshot:** 47321eb4c15f6d7ac5fa09ff307ec706e6021dd9
**База сравнения:** main = 20ce0d75928c3988628de93a784683a411307ff2
**PR:** #2, fix → main, открыт Draft

Документ составлен по всему дереву production source, test source, Android resources, Gradle configuration, manifest и GitHub Actions. Это статический архитектурный/кодовый аудит, а не доказательство отсутствия runtime-дефектов. Результаты device QA отдельно перечислены ниже.

## 1. Итог

Основные архитектурные этапы выполнены:

- Domain, data, render, UI и audio разделены по слоям.
- Доменная симуляция не импортирует Android API.
- GameView сокращён с 712 до 145 строк и оставлен Android View adapter.
- GameRenderer разделён на façade, MenuScreenRenderer и GameplayRenderer; shared Paint/Path и Canvas primitives находятся в GameRenderResources.
- Session, navigation и pointer routing вынесены в отдельные контроллеры.
- Добавлены строгая версия формата сохранения, all-or-nothing decoding, валидация progression и безопасная обработка неправильных типов SharedPreferences.
- Исправлен haptic отклик бустера: он учитывает настройку вибрации.
- Тестовый набор содержит 58 JVM-тестов в 10 файлах.

**Вывод:** архитектура существенно улучшена, основные границы слоёв выдержаны, критических нарушений domain/data/render separation при статическом просмотре не обнаружено. Но утверждать, что проект уже везде использует лучшие практики и полностью готов к релизу, нельзя. Ниже перечислены конкретные долги, которые лучше исправить отдельным этапом.

## 2. Что уже сделано хорошо

| Область | Подтверждённое состояние |
|---|---|
| Domain | GameEngine и GameModels не зависят от Android SDK; Random можно внедрять, игровые тесты используют seed |
| Data | Сохранение отделено от домена; schema 1, legacy schema 0, all-or-nothing parser, validation и monotonic progression |
| Коллекции домена | Внешние consumers получают unmodifiable live views для balls/projectiles |
| UI | GameView отвечает за Android View, Canvas и преобразование координат; session/navigation/input имеют отдельные owners |
| Simulation | GameLoopController использует fixed 60 Hz, ограничение frame delta и bounded catch-up |
| Rendering | Экранные и игровые слои разделены; Paint/Path переиспользуются через общий ресурсный объект |
| Lifecycle/audio | Activity делегирует resume/pause/destroy; ToneGenerator освобождается при teardown |
| Privacy | Приложение офлайн, INTERNET permission отсутствует, локальные настройки и save хранятся в SharedPreferences |
| CI | GitHub Actions собирает APK/AAB, запускает JVM tests и Android lint |
| Regression tests | Есть tests для saves/progression, simulation loop, input routing, Activity startup, renderer smoke и haptics preference |

## 3. Найденные улучшения для следующего этапа

### P1 — сначала исправить

| ID | Наблюдение и доказательство | Рекомендованное действие |
|---|---|---|
| P1-1 | **Сборка не использует стандартный Gradle Wrapper.** В gradle/wrapper находится только gradle-wrapper.properties, wrapper JAR отсутствует. Скрипты gradlew и gradlew.bat самостоятельно скачивают Gradle 8.13, жёстко задают версию и не проверяют SHA-256. Версия в скриптах и properties дублируется. CI вызывает установленную команду gradle, а не project bootstrap. | Перейти на стандартный Gradle Wrapper с wrapper JAR, одной точкой фиксации версии и distributionSha256Sum. В CI выполнять все задачи через ./gradlew; проверить Windows wrapper теми же задачами. |
| P1-2 | **Локализация хранится в коде.** res/values/strings.xml содержит только app_name; values-en отсутствует. RU/EN пары, tutorial copy, privacy text и accessibility labels находятся в renderer/navigation Kotlin. Это усложняет обновление текста, проверку полноты переводов и поддержку Android locale/configuration changes. | Перенести весь UI copy в values/strings.xml и values-en/strings.xml. Включить tutorial, HUD, boosters, overlays, settings, privacy и content descriptions. Добавить тест, сравнивающий набор resource keys для обоих языков. |
| P1-3 | **Нет end-to-end Android UI tests и подтверждённого device QA.** Сейчас тесты JVM/Robolectric. Нет src/androidTest, Espresso/UIAutomator сценариев или device matrix. Наличие renderer smoke test не доказывает правильность расположения controls, touch hit areas, TalkBack, системных панелей и lifecycle на реальном устройстве. | Добавить instrumentation tests для меню → карта → уровень, tutorial, booster, pause/resume, win/loss, settings, save/restore и accessibility click. Затем пройти device matrix: минимум API 23, промежуточный Android и API 36; отдельно проверить Xiaomi/MIUI, TalkBack, touch и звук/вибрацию. |

### P2 — следующий архитектурный и performance проход

| ID | Наблюдение и доказательство | Рекомендованное действие |
|---|---|---|
| P2-1 | **GameSessionController всё ещё широк по обязанностям** (около 306 строк): session lifecycle, save/progress, settings preferences, GameEvent side effects, audio, loop и transient effects. Это рабочий coordinator, но изменения настроек, звука и эффектов трудно тестировать независимо. | Выделить SettingsRepository/SettingsStore, GameEventHandler и EffectsController. Оставить GameSessionController фасадом, координирующим use cases. Добавить unit tests каждого collaborator. |
| P2-2 | **GameNavigationController совмещает переходы, hit testing и построение accessibility targets** (около 372 строк). Одна функция строит labels, bounds и callbacks для всех экранов. Часть экранных координат также описана в renderer. | Вынести GameAccessibilityTargetFactory/экранные target descriptors. По возможности определить единый screen-control layout, из которого берут bounds и обычный touch, и accessibility. Добавить тесты на уникальность ID, bounds и actions для каждого экрана. |
| P2-3 | **GameEngine остаётся крупным domain aggregate** (629 строк, около 32 методов): движение цепочки, projectiles/collision/insertion, spacing, matches/cascades, retraction, boosters и save/restore. Сейчас он Android-independent и тестируем, поэтому сам размер не является дефектом. | При следующем изменении правил выделять внутренние ChainMovement/ProjectileCollision/MatchResolver/Retraction components за стабильным GameEngine API. Не разбивать механически и не переносить правила в UI. |
| P2-4 | **Есть кандидаты на лишние allocations в render/frame path.** В renderer встречаются временные RectF/Path, списки/Triple и строки; tutorial/privacy/booster data создаётся внутри draw calls. GameSessionController.buildRenderState() создаёт ParticleVisual и ClearingBallVisual copies через map/toList для render snapshot. Это статические наблюдения, не доказанное узкое место. | Сначала измерить allocations и frame time через Android Studio Profiler или Macrobenchmark на слабом устройстве. По результатам вынести неизменные таблицы в static vals, кешировать геометрию/RectF где это оправдано, уменьшить copies на кадр. Не добавлять кеши без измерений. |
| P2-5 | **Simulation и side effects запускаются из View drawing path.** GameView.onDraw вызывает advanceFrame; внутри него обновляется симуляция, обрабатываются GameEvent и могут происходить save/progress side effects. Это затрудняет независимое моделирование frame scheduling и rendering. | Рассмотреть Choreographer/FrameCallback или отдельный frame scheduler: tick → event handling → render snapshot → draw. Сохранить fixed-step/catch-up semantics и добавить lifecycle/pause/resume tests. |
| P2-6 | **Persistence contract частично завязан на magic keys.** Имена SharedPreferences ключей повторяются по save/restore/clear. Ключ saved_paused записывается и удаляется, но restore задаёт paused напрямую и не читает его. | Централизовать key names/schema constants. Проверить legacy совместимость и удалить либо мигрировать saved_paused, если он действительно не нужен. Добавить тест миграции и полного набора ключей. |
| P2-7 | **Renderer smoke test не проверяет визуальную корректность.** GameRendererTest прогоняет верхнеуровневые экраны без exception, но не сравнивает screenshot/golden и не покрывает все pause/win/loss overlays, разные размеры viewport и длинные тексты. | Добавить screenshot/golden tests или детерминированные Canvas assertions для ключевых экранов и overlays. Проверить RU/EN, маленький viewport и text clipping. |

### P3 — укрепление качества и сопровождения

| ID | Наблюдение и доказательство | Рекомендованное действие |
|---|---|---|
| P3-1 | **Нет отдельного Kotlin style/static-analysis gate.** В CI есть Android lint, но нет ktlint/detekt или аналогичного Kotlin source quality gate. | Подключить один formatter/linter (например, ktlint) и один статический анализатор (например, detekt) с постепенным baseline; не вводить оба инструмента без согласованных правил. |
| P3-2 | **API matrix узкая.** Robolectric tests закреплены на API 35; targetSdk 36 и minSdk 23 не покрыты автоматической матрицей. | Добавить emulator/instrumented matrix по поддерживаемым API. Минимальный Android 23 особенно важен для legacy system UI/Back fallback. |
| P3-3 | **Privacy copy дублируется в нескольких местах.** Есть Markdown, HTML и текст внутри приложения. При изменении политики эти источники могут разойтись. | Сделать единый источник текста или добавить consistency test/checklist, сверяющий три представления. |
| P3-4 | **Совместимые package typealiases требуют явного решения.** В старом game package остались thin aliases для GameEngine/GameAudio/GameView. GameView alias используется MainActivity; Engine/Audio aliases не используются внутри приложения, но могут быть внешним API. | Зафиксировать, является ли старый package API поддерживаемым. Если да — документировать; если нет — обновить imports и удалить неиспользуемые aliases в отдельном breaking-change этапе. |
| P3-5 | **Некоторые конфигурационные решения требуют подтверждения тестом.** В manifest задан configChanges orientation/screenSize/keyboardHidden, но собственного onConfigurationChanged handler нет. Приложение portrait-only, поэтому это может быть избыточно. | Проверить поведение при смене конфигурации/сворачивании и удалить configChanges, если ручная обработка не требуется. |
| P3-6 | **Тестовая конфигурация разрешает default return values для Android stubs.** В build.gradle.kts установлено unitTests.isReturnDefaultValues = true. В связке с Robolectric нужно убедиться, что это не маскирует вызовы Android API без корректного shadow/runtime. | Проверить необходимость опции; по возможности отключить её и исправить тесты, которые не должны полагаться на нулевые значения Android stubs. |

## 4. Проверки, которые обязательно повторить после исправления backlog

- ./gradlew testDebugUnitTest
- ./gradlew lintDebug
- ./gradlew assembleDebug
- ./gradlew bundleRelease
- Сравнить CI head SHA с PR head SHA; не переносить результат старого commit на новый.
- Запустить device QA matrix из docs/RELEASE_CHECKLIST.md.
- Проверить TalkBack, accessibility node bounds/actions, background/foreground, touch mapping, звук/вибрацию и frame time.

## 5. Рекомендуемый порядок следующего этапа

1. P1-1: стандартный и проверяемый Gradle Wrapper + CI через wrapper.
2. P1-2: полный перенос copy в RU/EN Android string resources и автоматическая проверка resource parity.
3. P1-3: instrumentation/UI tests и device QA.
4. P2-1/P2-2/P2-6: выделение settings, event/effects и accessibility layout boundaries.
5. P2-4/P2-5: профилирование allocations/frame scheduling и оптимизация по результатам.
6. P2-3/P2-7: декомпозиция GameEngine только при сохранении domain API; screenshot/golden validation renderer.
7. P3: style/static analysis, API matrix, privacy copy consistency и конфигурационные проверки.

Приоритеты обозначают порядок работ, а не доказанную тяжесть каждого пункта. В частности, performance allocations следует подтверждать профилированием прежде, чем оптимизировать.

## 6. Ограничения этого аудита

Статическая проверка исходников и зелёный CI не могут подтвердить:
- отсутствие проблем на каждом OEM/Android release;
- качество визуального layout на всех плотностях/соотношениях сторон;
- реальное удобство TalkBack и switch access;
- отсутствие jank на слабых устройствах;
- соответствие публичной privacy policy всем будущим изменениям продукта.

Эти пункты требуют физического устройства, профилирования и отдельного release review.