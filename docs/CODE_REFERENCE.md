# Code Reference — Marble Frog

Документ служит картой исходного кода и описывает текущие контракты между слоями.

## Production source

| Файл | Ответственность | Основные точки входа |
|---|---|---|
| MainActivity.kt | Activity lifecycle, immersive mode, системная Back-навигация | onCreate, configureImmersiveMode, dispatchBack |
| game/domain/GameEngine.kt | Чистая Kotlin-симуляция и состояние кампании; публичные balls/projectiles — unmodifiable live views | startNewGame, startLevel, startTutorial, update, activateBooster, restoreState |
| game/domain/LevelCatalog.kt | Параметры уровней и геометрия дорожек | definition, route |
| game/domain/model/GameModels.kt | Модели, статусы, инвентарь, snapshot, события, OfflineStatistics и OfflineAchievement | Vec2, Ball, Projectile, GameSaveState, GameEvent |
| game/data/GameSaveRepository.kt | Сохранение текущей кампании | save, restore, clear |
| game/data/GameSaveCodec.kt | Строгая сериализация цепочки | encodeBalls, decodeBalls |
| game/data/LevelProgressRepository.kt | Открытые уровни, звёзды и рекорд кампании | recordCompletion, updateBestScore |
| game/data/OfflineProgressRepository.kt | Пожизненная локальная статистика и 8 достижений; отдельно от campaign save | recordShot, recordMatch, recordBooster, recordLevelCompleted, unlockedAchievements |
| game/render/GameRenderer.kt | Небольшой Canvas façade: выбор слоя по GameScreen | draw |
| game/render/MenuScreenRenderer.kt | Меню, карта, обучение, настройки, privacy и Achievements/Statistics | drawMenu, drawLevelMap, drawTutorial, drawSettings, drawPrivacy, drawAchievements |
| game/render/GameplayRenderer.kt | Координатор игрового кадра, делегирует специализированным слоям | drawGame |
| game/render/TrackRenderer.kt | Геометрия дорожки и препятствия | drawTrack, drawObstacles |
| game/render/GameEntitiesRenderer.kt | Шары цепочки, снаряды и frog shooter | drawBalls, drawProjectiles, drawShooter |
| game/render/GameEffectsRenderer.kt | Исчезающие шары и частицы | drawClearingBalls, drawParticles |
| game/render/HudRenderer.kt | Счёт, панель статуса и бустеры | drawHud, drawBoosters |
| game/render/GameOverlayRenderer.kt | Pause, win/loss overlays | drawPauseOverlay, drawEndOverlay |
| game/render/GameRenderResources.kt | Общий Paint/Path cache, primitives и общий GameTextResources | drawBackground, drawMarble, drawFrog, button, centeredText |
| game/render/GameTextResources.kt | Кэшированные RU/EN Resources; явный язык из session не зависит от locale устройства | getString |
| game/ui/GameView.kt | Android View adapter: lifecycle, Canvas, coordinate mapping and platform events | onDraw, onTouchEvent, handleSystemBack |
| game/ui/GameSessionController.kt | Gameplay session, engine lifecycle, saves, progress, event side effects, audio and effects | resumeGame, pauseGame, fireShot, handlePendingEvents, saveGameState |
| game/ui/GameNavigationController.kt | Screen state, hit areas, transitions and virtual accessibility targets | handleSystemBack, handleTap, handleBoosterTap, accessibilityTargets |
| game/ui/GameInputRouter.kt | Platform-independent pointer gesture routing (aim, fire, booster and tap) | handle, cancel |
| game/ui/GameLoopController.kt | Fixed 60 Hz accumulator, frame-delta clamp and bounded catch-up | start, stop, resetAccumulator, advanceFrame |
| game/ui/ProgressPresentationCache.kt | Cached best score, unlock and per-level stars for presentation | recordCompletion, updateBestScore, isUnlocked |
| game/ui/GameAccessibilityNodeProvider.kt | Виртуальные accessibility-кнопки для Canvas | createAccessibilityNodeInfo, performAction |
| game/audio/GameAudio.kt | Офлайн-звуковые сигналы | playShot, playMatch, playWin, playLose, release |

## Жизненный цикл GameEngine

1. Конструктор инициализирует первый уровень.
2. startNewGame() сбрасывает очки, выстрелы и инвентарь и запускает уровень 1.
3. startLevel(id) начинает выбранный этап. UI обязан предварительно проверить LevelProgressRepository.isUnlocked(id).
4. startTutorial() создаёт отдельную фиксированную цепочку и тренировочный инвентарь.
5. update(dt) продвигает симуляцию только при статусе RUNNING.
6. pauseRound() / resumeRound() меняют доменный статус, не уничтожая текущую цепочку.
7. startNextLevel() требует WON и отклоняет тренировочные сессии.
8. saveState() возвращает settled snapshot: снаряды в полёте и визуальные частицы временные.
9. restoreState(snapshot) проверяет данные до замены активного состояния.

## Доменный поток событий

| GameEvent | Реакция UI |
|---|---|
| ShotFired | Звук выстрела |
| MatchCleared | Звук совпадения, fade-шары, частицы, сохранение |
| LevelStarted | Дополнительного UI side effect нет |
| LevelCompleted | Звук победы; звёзды/unlock и очистка save только для кампании |
| BoosterActivated | Click/haptic и сохранение |
| GameLost | Звук поражения и очистка обычного сохранения |

UI не должен дублировать игровые правила: он вызывает доменный метод, затем обрабатывает GameEvent.

## Соглашения сохранения

- GameSaveCodec schema 0 — старый формат без ключа версии; schema 1 — актуальный.
- Декодирование all-or-nothing. Если один token некорректен, вся цепочка отклоняется.
- GameEngine.restoreState дополнительно проверяет ranges, finite значения, порядок шаров, маршрут и заряды.
- После успешного legacy restore сохраняется marker schema 1.
- Обучение не сохраняет обычную партию и не очищает её при выходе.

## Кадровый цикл и ввод

1. Android вызывает GameView.onDraw(); View проверяет размер поверхности и считает преобразование screen pixels → world coordinates для мира 420×900.
2. GameView передаёт lifecycle/frame tick в GameSessionController; контроллер использует GameLoopController для фиксированных шагов 1/60 секунды с ограниченным catch-up.
3. GameSessionController владеет GameEngine, save/progress repositories, аудио, обработкой GameEvent и временными эффектами; он собирает GameRenderState из актуального session snapshot.
4. GameNavigationController владеет GameScreen/tutorial page, hit areas и переходами. Он также создаёт GameAccessibilityTarget с теми же действиями, что используются обычными касаниями.
5. GameInputRouter обрабатывает DOWN/MOVE/UP/CANCEL без Android-зависимостей и маршрутизирует aim/fire/booster/screen tap через callbacks.
6. GameView только преобразует MotionEvent в логическую точку, передаёт действие в router и вызывает GameRenderer.
7. GameAccessibilityNodeProvider публикует virtual targets, подготовленные GameNavigationController, как Android accessibility nodes.

## Добавление уровня

1. При необходимости добавить TrackId и новую полилинию в LevelCatalog.route().
2. Обновить диапазоны уровней, препятствия и параметры сложности в LevelCatalog.definition().
3. Добавить проверки маршрута, obstacle positions и progression в GameEngineTest.
4. Обновить GAME_DESIGN.md и TECHNICAL_DESIGN.md.
5. Проверить MAX_LEVELS, разблокировку последнего уровня и переходы между группами дорожек.

## Добавление бустера

1. Добавить значение BoosterType и соответствующий баланс зарядов в BoosterInventory.
2. Реализовать applicability guard в GameEngine.activateBooster(). Заряд потребляется только при выполнимом действии.
3. Добавить отображение и hit area в GameRenderer и обработку касания в GameView.
4. Добавить локализованное имя и действие в GameAccessibilityNodeProvider.
5. Добавить JVM-тесты успешного действия, неподходящих предусловий, изменения заряда и событий.
6. Обновить GAME_DESIGN.md, TECHNICAL_DESIGN.md и RELEASE_CHECKLIST.md.

## Тесты

- GameEngineTest.kt: движение, последовательный вход цепочки с начала дорожки, плавная impact-вставка с разъездом соседей, актуальная палитра ammo, совпадения/каскады, pause/restore, progression, маршруты и бустеры.
- GameLoopControllerTest.kt: фиксированный шаг 60 Hz, ограниченный catch-up, reset accumulator и pause/resume без симуляции времени в фоне.
- GameInputRouterTest.kt: drag-to-aim, fire on release, isolated booster gesture and cancellation.
- MainActivityTest.kt: Robolectric regression test for Activity startup after DecorView/content initialization.
- ProgressPresentationCacheTest.kt: начальный snapshot, update после completion и монотонное обновление рекорда.
- GameRendererTest.kt: smoke test отрисовки всех верхнеуровневых экранов с общим render resource instance.
- GameTextResourcesTest.kt: одинаковый набор RU/EN string resource keys и выбор языка игры независимо от device locale.
- GameSessionControllerTest.kt: настройка вибрации при haptic feedback и исключение tutorial/practice из lifetime statistics.
- OfflineProgressRepositoryTest.kt: counters, best stars/score, unlock criteria, corrupt/invalid inputs, persistence and reset.
- GameAccessibilityInstrumentedTest.kt: Activity/accessibility tree, provider actions, real UiDevice taps through menu/settings/privacy/map/tutorial/achievements.
- GameSaveCodecTest.kt: legacy round-trip, malformed tokens, blank payload и полнота цепочки.
- GameSaveRepositoryTest.kt: save/restore, upgrade неверсированной schema 0, повреждённые payload/типы SharedPreferences, будущая schema и защита tutorial save.
- LevelProgressRepositoryTest.kt: monotonic unlock/stars/score, валидация входа, legacy fallback и повреждённые SharedPreferences values.
- Repository tests используют Robolectric 4.17 на API 35; запуск: ./gradlew testDebugUnitTest и ./gradlew lintDebug.

## Известные точки дальнейшего рефакторинга

GameView оставлен тонким Android-адаптером. GameSessionController отвечает за сессию, сохранения, прогресс и события; GameNavigationController — за экраны, переходы, hit areas и accessibility targets; GameInputRouter — за pointer gestures.

GameRenderer разделён на façade, MenuScreenRenderer и GameplayRenderer. GameplayRenderer координирует TrackRenderer, GameEntitiesRenderer, GameEffectsRenderer, HudRenderer и GameOverlayRenderer. Все слои используют общий GameRenderResources instance; renderer layers не владеют вводом или сохранениями. Следующая архитектурная задача — снизить связанность session/navigation controllers и вынести доступность/компоновку controls в единое описание.

Тексты RU/EN хранятся в Android string resources: `res/values/strings.xml` и `res/values-en/strings.xml`. `GameTextResources` кэширует locale-specific Resources и использует язык из session независимо от locale устройства. Полный приоритетный список улучшений находится в docs/REFACTOR_AUDIT.md.

SharedPreferences repositories покрыты Robolectric integration tests. Физическое устройство всё ещё требуется для проверки FPS, TalkBack, touch scaling, звука/вибрации и поведения при background/foreground.
