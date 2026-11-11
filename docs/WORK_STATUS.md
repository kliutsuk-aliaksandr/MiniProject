# Технический аудит — текущее состояние Marble Frog

**Снимок:** 11 ноября 2026 года
**Рабочая ветка:** dev (ответвлена от fix)
**Кодовый snapshot:** 026fc8d0227d31c545dcda26171d3e842e724d44
**База:** main = 20ce0d75928c3988628de93a784683a411307ff2
**PR №2:** открыт, Draft, fix → main

Ветка dev унаследовала текущий результат fix и теперь содержит стандартный Gradle Wrapper 8.13 с проверенными checksum, CI через ./gradlew и локализацию RU/EN через Android string resources. main и fix не изменялись; PR №2 остаётся Draft.

## 1. Что выполнено

- GameView сокращён с 712 до 145 строк и оставлен Android View adapter.
- GameSessionController владеет session lifecycle, simulation loop, repositories, audio, GameEvent side effects и transient effects.
- GameNavigationController владеет экранами, переходами, hit areas и accessibility targets.
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
| GameSessionController.kt | 306 | Session lifecycle, persistence, events, audio/effects |
| GameNavigationController.kt | 372 | Screen state, transitions, hit areas, accessibility targets |
| GameInputRouter.kt | 68 | Pointer gesture router |
| GameEngine.kt | 629 | Pure Kotlin domain aggregate |
| GameRenderer.kt | 59 | Renderer façade |
| MenuScreenRenderer.kt | 267 | Menu/map/tutorial/settings/privacy |
| GameplayRenderer.kt | 54 | Gameplay draw coordinator |
| TrackRenderer.kt | 79 | Route and obstacle rendering |
| GameEntitiesRenderer.kt | 28 | Chain/projectiles/frog |
| GameEffectsRenderer.kt | 38 | Clearing and particles |
| HudRenderer.kt | 81 | Score/status/boosters |
| GameOverlayRenderer.kt | 69 | Pause/win/loss overlays |
| GameRenderResources.kt | 200 | Shared Paint/Path/Canvas resources |

Render layers share one resource instance. They do not own input, persistence or domain rules. Domain source remains free of Android imports.

## 3. Автоматические тесты

Текущий test source содержит **60 JVM-тестов в 11 файлах**:

| Test class | Тестов |
|---|---:|
| GameEngineTest | 28 |
| GameSaveCodecTest | 4 |
| GameSaveRepositoryTest | 7 |
| LevelProgressRepositoryTest | 6 |
| GameLoopControllerTest | 3 |
| ProgressPresentationCacheTest | 3 |
| MainActivityTest | 1 |
| GameInputRouterTest | 3 |
| GameSessionControllerTest | 2 |
| GameRendererTest | 1 |
| GameTextResourcesTest | 2 |
| **Итого** | **60** |

Robolectric tests используют API 35. App minSdk 23 / targetSdk 36 не менялись.

## 4. CI

Текущий dev snapshot `026fc8d0227d31c545dcda26171d3e842e724d44` прошёл полный [Android CI](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42988455144):

- Linux: `./gradlew assembleDebug`, `testDebugUnitTest` (60 tests), `lintDebug`, `bundleRelease`, upload debug APK и unsigned release AAB — success.
- Windows: `gradlew.bat --version` — success.
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

- main остаётся на 20ce0d75928c3988628de93a784683a411307ff2.
- Текущие исправления выполняются fast-forward в dev; fix оставлена неизменной как база Draft PR.
- PR №2 остаётся Draft; merge не выполнялся.
- main и fix остаются нетронутыми; dev содержит полный текущий результат и дополнительные исправления.

Дальнейший порядок работ — по приоритетам из REFACTOR_AUDIT.md. Не следует снимать Draft до закрытия P1 и release/device gates.