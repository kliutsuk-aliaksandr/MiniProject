# Технический аудит — текущее состояние Marble Frog

**Снимок:** 11 ноября 2026 года
**Рабочая ветка:** fix
**Кодовый snapshot:** 47321eb4c15f6d7ac5fa09ff307ec706e6021dd9
**База:** main = 20ce0d75928c3988628de93a784683a411307ff2
**PR №2:** открыт, Draft, fix → main

Этот статус описывает кодовый snapshot перед синхронизацией документации. Последующий документационный commit не меняет production source. main не изменялся.

## 1. Что выполнено

- GameView сокращён с 712 до 145 строк и оставлен Android View adapter.
- GameSessionController владеет session lifecycle, simulation loop, repositories, audio, GameEvent side effects и transient effects.
- GameNavigationController владеет экранами, переходами, hit areas и accessibility targets.
- GameInputRouter маршрутизирует DOWN/MOVE/UP/CANCEL и остаётся независимым от Android API.
- GameRenderer стал небольшим façade; MenuScreenRenderer отвечает за меню/карту/обучение/настройки/privacy, GameplayRenderer — за игровое поле/HUD/бустеры/overlays, GameRenderResources — за shared Paint/Path и Canvas primitives.
- Общий базовый renderer и ресурсы не входят в публичный API; GameRenderer использует composition.
- Fixed-step simulation loop и progression presentation выделены в GameLoopController и ProgressPresentationCache.
- Save codec использует schema 1, legacy schema 0, strict all-or-nothing parser; репозитории валидируют повреждённые данные и typed preferences.
- Исправлены Bomb aim target, tutorial isolation, booster applicability, monotonic progression и haptic preference handling.
- Исправлен startup crash при настройке immersive mode до создания DecorView.
- Обновлены README, CODE_REFERENCE, TECHNICAL_DESIGN, ROADMAP и CHANGELOG; полный список remaining improvements находится в REFACTOR_AUDIT.md.

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
| GameplayRenderer.kt | 288 | Track/entities/HUD/boosters/effects/overlays |
| GameRenderResources.kt | 200 | Shared Paint/Path and Canvas primitives |

Domain source не импортирует Android API. Renderers не обрабатывают ввод и не читают SharedPreferences. Shared Paint/Path cache переиспользуется через один resource instance на GameRenderer.

## 3. Автоматические тесты

Текущий test source содержит **58 JVM-тестов в 10 файлах**:

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
| **Итого** | **58** |

Robolectric tests используют API 35. App minSdk 23 / targetSdk 36 не менялись.

## 4. CI

На текущем кодовом snapshot 47321eb4c15f6d7ac5fa09ff307ec706e6021dd9 оба workflow завершились success:

- [Push CI](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42979240130)
- [PR CI](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42979245640)

Оба прошли assembleDebug, testDebugUnitTest, lintDebug, bundleRelease и upload debug APK / unsigned release AAB.

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
- Все изменения выполнялись fast-forward в fix.
- PR №2 остаётся Draft; merge не выполнялся.
- Сравнение snapshot 47321... с main: 27 commits ahead, 0 behind.

Дальнейший порядок работ — по приоритетам из REFACTOR_AUDIT.md. Не следует снимать Draft до закрытия P1 и release/device gates.