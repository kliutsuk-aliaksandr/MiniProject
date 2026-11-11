# Полный аудит рефакторинга — Marble Frog

**Дата аудита:** 11 ноября 2026 года
**Проверенная ветка:** dev (создана от fix)
**Кодовый snapshot:** 026fc8d0227d31c545dcda26171d3e842e724d44
**База сравнения:** main = 20ce0d75928c3988628de93a784683a411307ff2
**PR:** #2, fix → main, открыт Draft

Документ составлен по всему дереву production source, test source, Android resources, Gradle configuration, manifest и GitHub Actions. Это статический архитектурный/кодовый аудит, а не доказательство отсутствия runtime-дефектов. Результаты device QA отдельно перечислены ниже.

## Дополнение: gameplay polish и первый срез Этапа 7 — 11 ноября 2026

Кодовый snapshot feature-ветки: `14cccb21aa9a280c52d52f7f393bb6993f7ccf68`. Полный [GitHub Actions PR run 43031140238](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/43031140238) завершился успешно: 5 из 5 jobs.

- Цепочка начинает движение от входа маршрута; отрицательный progress сохраняется как позиция шаров, которые ещё не вышли на экран.
- Вставка запускает отдельную короткую impact-анимацию: соседние шары разъезжаются, входящий занимает промежуток; каскад проверяется после посадки.
- Ammo и next ammo нормализуются по множеству цветов в текущей цепочке.
- Добавлены локальные lifetime statistics и 8 achievements в отдельном `offline_progress`; tutorial/practice не учитывается.
- Добавлен экран Achievements & Statistics в RU/EN и обновлены Canvas primitives/HUD/menu.
- JVM-набор расширен с 60 до 69 тестов; Android instrumentation — 3 сценария на API 23/29/36 (9 успешных emulator executions).

Эта проверка подтверждает CI и эмуляторные сценарии, но не ручной QA на физических устройствах. TalkBack/switch access в реальной среде, Xiaomi/MIUI, различные плотности, background/foreground, FPS и звук/вибрация остаются открытыми release checks.

## 1. Итог

Основные архитектурные этапы выполнены:

- Domain, data, render, UI и audio разделены по слоям.
- Доменная симуляция не импортирует Android API.
- GameView сокращён с 712 до 145 строк и оставлен Android View adapter.
- GameRenderer разделён на façade и MenuScreenRenderer; GameplayRenderer координирует TrackRenderer, GameEntitiesRenderer, GameEffectsRenderer, HudRenderer и GameOverlayRenderer. Общие Paint/Path/Canvas primitives находятся в GameRenderResources.
- Session, navigation и pointer routing вынесены в отдельные контроллеры.
- Добавлены строгая версия формата сохранения, all-or-nothing decoding, валидация progression и безопасная обработка неправильных типов SharedPreferences.
- Исправлен haptic отклик бустера: он учитывает настройку вибрации.
- Тестовый набор содержит 79 JVM-тестов в 13 файлах (механика, offline achievements/statistics и challenge mode).

**Вывод:** архитектура существенно улучшена, основные границы слоёв выдержаны, критических нарушений domain/data/render separation при статическом просмотре не обнаружено. Но утверждать, что проект уже везде использует лучшие практики и полностью готов к релизу, нельзя. Ниже перечислены конкретные долги, которые лучше исправить отдельным этапом.

## 2. Что уже сделано хорошо

| Область | Подтверждённое состояние |
|---|---|
| Domain | GameEngine и GameModels не зависят от Android SDK; Random можно внедрять, игровые тесты используют seed |
| Data | Сохранение отделено от домена; schema 1, legacy schema 0, all-or-nothing parser, validation и monotonic progression |
| Коллекции домена | Внешние consumers получают unmodifiable live views для balls/projectiles |
| UI | GameView отвечает за Android View, Canvas и преобразование координат; session/navigation/input имеют отдельные owners |
| Simulation | GameLoopController использует fixed 60 Hz, ограничение frame delta и bounded catch-up |
| Rendering | MenuScreenRenderer и отдельные track/entities/effects/HUD/overlay renderers; Paint/Path переиспользуются через общий ресурсный объект |
| Lifecycle/audio | Activity делегирует resume/pause/destroy; ToneGenerator освобождается при teardown |
| Privacy | Приложение офлайн, INTERNET permission отсутствует, локальные настройки и save хранятся в SharedPreferences |
| CI | GitHub Actions собирает APK/AAB, запускает JVM tests и Android lint |
| Regression tests | Есть tests для saves/progression, simulation loop, input routing, Activity startup, renderer smoke и haptics preference |

## 3. Найденные улучшения для следующего этапа

### P1 — сначала исправить

| ID | Наблюдение и доказательство | Рекомендованное действие |
|---|---|---|
| P1-1 | **ИСПРАВЛЕНО в dev.** В коммите `34ad19e` добавлены официальные Gradle 8.13 `gradle-wrapper.jar`, `gradlew` и `gradlew.bat`; JAR сверён с опубликованным SHA-256 `81a82aa...ae45f`. В `gradle-wrapper.properties` закреплён SHA-256 дистрибутива `20f1b117...0aed78` (корректировка URL — `a5230be`). CI использует `./gradlew`; `setup-gradle@v6` автоматически проверяет wrapper JAR. Подтверждено [зелёным Linux + Windows CI](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42985668241): assembleDebug, 58 tests, lintDebug, bundleRelease, upload APK/AAB и `gradlew.bat --version` прошли. | Закрыто. При обновлении Gradle повторять проверку JAR и distribution checksum. |
| P1-2 | **ИСПРАВЛЕНО в dev.** 97 ключей UI-текста перенесены в `res/values/strings.xml` (RU) и `res/values-en/strings.xml` (EN): меню, карта уровней, tutorial, настройки, privacy, HUD, бустеры, gameplay hints, overlays и accessibility labels. `GameTextResources` создаёт кэшированные locale-specific Resources и использует явный выбор языка из session, независимо от locale устройства. Одна и та же инстанция передаётся renderer и navigation. Добавлены два теста: паритет ключей RU/EN и explicit language selection. Полный [Linux + Windows CI](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42988455144) прошёл: APK, 60 unit tests, lint, AAB, artifact upload и Windows Wrapper smoke test. | Закрыто. Новые строки добавлять сразу в оба resource-файла; parity test обязателен. |
| P1-3 | **Эмуляторный instrumentation/UIAutomator слой добавлен и проходит CI; physical-device QA остаётся открытым.** 3 сценария проверяют MainActivity/accessibility tree, actions AccessibilityNodeProvider и внешние координатные касания menu/settings/privacy/language/map/tutorial/achievements. API 23/29/36 прошли в [CI run 43028846127](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/43028846127), всего 9 успешных executions. Это не подтверждает реальную работу TalkBack/switch access или OEM-specific поведение. | Оставить emulator matrix как regression gate. Отдельно пройти Xiaomi/MIUI, TalkBack, switch access, density/touch, background/foreground, save/restore, звук/вибрацию и FPS на физических устройствах. Дополнить external-touch gameplay hit-area checks для insertion, booster, win/loss. |

### P2 — следующий архитектурный и performance проход

| ID | Наблюдение и доказательство | Рекомендованное действие |
|---|---|---|
| P2-1 | **GameSessionController всё ещё широк по обязанностям** (339 строк): session lifecycle, save/progress, settings preferences, GameEvent side effects, audio, loop и transient effects. Это рабочий coordinator, но изменения настроек, звука и эффектов трудно тестировать независимо. | Выделить SettingsRepository/SettingsStore, GameEventHandler и EffectsController. Оставить GameSessionController фасадом, координирующим use cases. Добавить unit tests каждого collaborator. |
| P2-2 | **GameNavigationController совмещает переходы, hit testing и построение accessibility targets** (487 строк). Одна функция строит labels, bounds и callbacks для всех экранов. Часть экранных координат также описана в renderer. | Вынести GameAccessibilityTargetFactory/экранные target descriptors. По возможности определить единый screen-control layout, из которого берут bounds и обычный touch, и accessibility. Добавить тесты на уникальность ID, bounds и actions для каждого экрана. |
| P2-3 | **GameEngine остаётся крупным domain aggregate** (770 строк): движение цепочки, projectiles/collision/insertion, spacing, matches/cascades, retraction, boosters и save/restore. Сейчас он Android-independent и тестируем, поэтому сам размер не является дефектом. | При следующем изменении правил выделять внутренние ChainMovement/ProjectileCollision/MatchResolver/Retraction components за стабильным GameEngine API. Не разбивать механически и не переносить правила в UI. |
| P2-4 | **Есть кандидаты на лишние allocations в render/frame path.** В renderer встречаются временные RectF/Path, списки/Triple и строки; tutorial/privacy/booster data создаётся внутри draw calls. GameSessionController.buildRenderState() создаёт ParticleVisual и ClearingBallVisual copies через map/toList для render snapshot. Это статические наблюдения, не доказанное узкое место. | Сначала измерить allocations и frame time через Android Studio Profiler или Macrobenchmark на слабом устройстве. По результатам вынести неизменные таблицы в static vals, кешировать геометрию/RectF где это оправдано, уменьшить copies на кадр. Не добавлять кеши без измерений. |
| P2-5 | **Simulation и side effects запускаются из View drawing path.** GameView.onDraw вызывает advanceFrame; внутри него обновляется симуляция, обрабатываются GameEvent и могут происходить save/progress side effects. Это затрудняет независимое моделирование frame scheduling и rendering. | Рассмотреть Choreographer/FrameCallback или отдельный frame scheduler: tick → event handling → render snapshot → draw. Сохранить fixed-step/catch-up semantics и добавить lifecycle/pause/resume tests. |
| P2-6 | **Persistence contract частично завязан на magic keys.** Имена SharedPreferences ключей повторяются по save/restore/clear. Ключ saved_paused записывается и удаляется, но restore задаёт paused напрямую и не читает его. | Централизовать key names/schema constants. Проверить legacy совместимость и удалить либо мигрировать saved_paused, если он действительно не нужен. Добавить тест миграции и полного набора ключей. |
| P2-7 | **Renderer smoke test не проверяет визуальную корректность.** GameRendererTest прогоняет верхнеуровневые экраны без exception, но не сравнивает screenshot/golden и не покрывает все варианты pause/win/loss overlays, разные размеры viewport и длинные тексты. | Добавить screenshot/golden tests или детерминированные Canvas assertions для ключевых экранов и overlays. Проверить RU/EN, маленький viewport и text clipping. |

### P3 — укрепление качества и сопровождения

| ID | Наблюдение и доказательство | Рекомендованное действие |
|---|---|---|
| P3-1 | **Нет отдельного Kotlin style/static-analysis gate.** В CI есть Android lint, но нет ktlint/detekt или аналогичного Kotlin source quality gate. | Подключить один formatter/linter (например, ktlint) и один статический анализатор (например, detekt) с постепенным baseline; не вводить оба инструмента без согласованных правил. |
| P3-2 | **Android emulator matrix добавлена.** Instrumentation запускается на API 23, 29 и 36; все три API прошли в [CI run 43028846127](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/43028846127). Robolectric tests используют API 35; matrix не заменяет OEM/physical-device QA. | Пересматривать API matrix при изменении minSdk/targetSdk; отдельно тестировать Xiaomi/MIUI и реальные устройства. |
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

1. [x] P1-1: стандартный и проверяемый Gradle Wrapper + CI через wrapper (полный Linux/Windows CI: 42985668241).
2. [x] P1-2: полный перенос copy в RU/EN Android string resources и автоматическая проверка resource parity (97 ключей, 2 теста, CI 42988455144).
3. P1-3: emulator instrumentation/UIAutomator coverage и API 23/29/36 пройдены; P1-3 остаётся открытым только по физическим устройствам, TalkBack/switch access, OEM, FPS и gameplay touch/lifecycle gates.
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