# Технический аудит — текущее состояние Marble Frog

**Снимок:** 11 ноября 2026 года
**Рабочая feature-ветка:** feature/gameplay-polish-offline-content → dev → main
**Кодовый snapshot feature:** 305d0d4adf8bfa6e234d4995800492eb10c267f4
**База:** main = 20ce0d75928c3988628de93a784683a411307ff2
**PR №3:** открыт, Draft, feature/gameplay-polish-offline-content → dev; PR №2 закрыт как устаревший, без merge

Рабочий поток организован как feature/* → dev → main. Текущая feature-ветка ответвлена от dev (30 коммитов сверх dev на момент проверки). Ветка dev остаётся на 93dc1c6, main — на 20ce0d7, fix — на a2a48d6. PR №3 открыт Draft feature → dev; старый PR №2 fix → main закрыт как устаревший и не сливался. Ветки main/dev/fix не обновлялись этой feature-работой.

## 1. Что выполнено

- GameView сокращён с 712 до 145 строк и оставлен Android View adapter.
- GameSessionController владеет session lifecycle, simulation loop, campaign/offline/challenge repositories, audio, GameEvent side effects и transient effects.
- GameNavigationController владеет экранами, переходами, hit areas и accessibility targets, включая список испытаний и challenge-result actions.
- GameInputRouter маршрутизирует DOWN/MOVE/UP/CANCEL и остаётся независимым от Android API.
- GameRenderer является небольшим façade; MenuScreenRenderer отвечает за menu/map/tutorial/settings/privacy. GameTextResources кэширует RU/EN Resources и передаёт выбранный язык renderer/navigation.
- GameplayRenderer координирует TrackRenderer, GameEntitiesRenderer, GameEffectsRenderer, HudRenderer и GameOverlayRenderer.
- GameRenderResources владеет общим Paint/Path/Shader cache и переиспользуемыми Canvas primitives. Внутренний базовый renderer не входит в public API.
- Fixed-step simulation loop и progression presentation выделены в GameLoopController и ProgressPresentationCache.
- Save codec использует schema 1, legacy schema 0, strict all-or-nothing parser; repositories валидируют повреждённые данные и typed preferences.
- Исправлены Bomb aim target, tutorial isolation, booster applicability, monotonic progression и haptic preference handling.
- Исправлен startup crash при настройке immersive mode до создания DecorView.
- Полный список remaining improvements находится в REFACTOR_AUDIT.md.

## 2. Архитектура и размеры ключевых файлов

| Файл | Строк | Роль |
|---|---:|---|
| GameView.kt | 145 | Android View / Canvas adapter |
| GameSessionController.kt | 339 | Session lifecycle, persistence, campaign/offline/challenge progress, events, audio/effects |
| GameNavigationController.kt | 487 | Screen state, transitions, hit areas, accessibility targets incl. achievements/challenges |
| GameInputRouter.kt | 68 | Pointer gesture router |
| GameEngine.kt | 770 | Pure Kotlin domain aggregate; route entrance, animated insertion, live shot palette and isolated challenges |
| GameRenderer.kt | 68 | Renderer façade incl. achievements and challenges |
| MenuScreenRenderer.kt | 345 | Menu/map/challenges/tutorial/settings/privacy/achievements |
| GameplayRenderer.kt | 54 | Gameplay draw coordinator |
| TrackRenderer.kt | 79 | Route and obstacle rendering |
| GameEntitiesRenderer.kt | 28 | Chain/projectiles/frog |
| GameEffectsRenderer.kt | 38 | Clearing and particles |
| HudRenderer.kt | 98 | Score/status/boosters and challenge limits |
| GameOverlayRenderer.kt | 103 | Pause/win/loss overlays and challenge-specific results |
| GameRenderResources.kt | 235 | Shared Paint/Path/Canvas resources and polished UI primitives |
| OfflineProgressRepository.kt | 127 | Local lifetime statistics and achievement unlocks, isolated from campaign save |
| ChallengeCatalog.kt | 28 | Three immutable offline challenge definitions with shot/time constraints |
| ChallengeProgressRepository.kt | 69 | Local best stars, shots and remaining time for challenges |

Render layers share one resource instance. They do not own input, persistence or domain rules. Domain source remains free of Android imports.

## 3. Автоматические тесты

Текущий test source содержит **79 JVM-тестов в 13 файлах**:

| Test class | Тестов |
|---|---:|
| GameEngineTest | 36 |
| GameSaveCodecTest | 4 |
| GameSaveRepositoryTest | 7 |
| LevelProgressRepositoryTest | 6 |
| GameLoopControllerTest | 3 |
| ProgressPresentationCacheTest | 3 |
| MainActivityTest | 1 |
| GameInputRouterTest | 3 |
| GameSessionControllerTest | 4 |
| GameRendererTest | 1 |
| GameTextResourcesTest | 2 |
| OfflineProgressRepositoryTest | 5 |
| ChallengeProgressRepositoryTest | 4 |
| **Итого** | **79** |

Robolectric tests используют API 35. App minSdk 23 / targetSdk 36 не менялись.

### Android instrumentation

`GameAccessibilityInstrumentedTest.kt` содержит 3 сценария: MainActivity/accessibility tree, действия AccessibilityNodeProvider для settings/tutorial/boosters/Pause/Resume и UIAutomator taps по реальным bounds меню/settings/privacy/language/map/tutorial/achievements; challenge card, pause и challenge-list проверяются через virtual-node ACTION_CLICK. Все три прошли на API 23, 29 и 36 (9 успешных emulator executions).

## 4. CI

Feature snapshot `305d0d4adf8bfa6e234d4995800492eb10c267f4` прошёл полный [Android CI run 43047764093](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/43047764093) — 5/5 jobs success:

- Linux: `./gradlew assembleDebug`, `testDebugUnitTest` (79 JVM tests), `lintDebug`, `bundleRelease`, upload debug APK и unsigned release AAB — success.
- Windows: `gradlew.bat --version` — success.
- Android instrumentation: 3 теста на каждом из API 23, 29 и 36; все jobs прошли (9 успешных executions).
- Gradle `setup-gradle@v6` подтвердил валидность wrapper JAR и официальный checksum.
- Два теста локализации проверяют одинаковые RU/EN resource keys и explicit language selection независимо от системной locale.

## 5. Что не подтверждено на реальном устройстве

- Первый запуск и immersive system bars на Xiaomi/MIUI.
- Touch mapping и hit areas при разных densities/aspect ratios.
- TalkBack, switch access и virtual node actions.
- Background/foreground, force-stop restore, pause/resume.
- Звук/вибрация при toggles, booster и Activity teardown.
- FPS/frame time и allocations на слабом устройстве.

Статический audit backlog и рекомендуемый порядок исправлений находятся в [REFACTOR_AUDIT.md](REFACTOR_AUDIT.md).

## 6. Git safety

- Feature PR #3: `feature/gameplay-polish-offline-content → dev`, Draft.
- Старый PR #2 `fix → main` закрыт без merge.
- Rulesets: **не настроены**. Список repository rulesets через GitHub API вернул пустой массив; текущий GitHub connector поддерживает для Rulesets только GET и не предоставляет create/update action. Legacy branch-protection endpoint вернул 403. Точная конфигурация для ручного применения записана в [GITHUB_RULESET_SETUP.md](GITHUB_RULESET_SETUP.md).
- `.github/workflows/android-ci.yml` запускает CI на push только в `main/dev`, а feature-ветки — через `pull_request` в `main/dev`; это исключает двойной запуск одинаковой matrix для PR-ветки.

- main остаётся на 20ce0d75928c3988628de93a784683a411307ff2.
- Новые изменения идут через feature/* → dev → main; feature → dev review выполняется PR #3 (Draft).
- PR #2 fix → main закрыт как устаревший; merge не выполнялся. `fix` оставлена нетронутой для истории.
- main и fix остаются нетронутыми; dev содержит полный текущий результат и дополнительные исправления.

Дальнейший порядок работ — по приоритетам из REFACTOR_AUDIT.md. Не следует снимать Draft до закрытия P1 и release/device gates.