# Code Reference — Marble Frog

Документ служит картой исходного кода и описывает текущие контракты между слоями.

## Production source

| Файл | Ответственность | Основные точки входа |
|---|---|---|
| MainActivity.kt | Activity lifecycle, immersive mode, системная Back-навигация | onCreate, configureImmersiveMode, dispatchBack |
| game/domain/GameEngine.kt | Чистая Kotlin-симуляция и состояние кампании | startNewGame, startLevel, startTutorial, update, activateBooster, restoreState |
| game/domain/LevelCatalog.kt | Параметры уровней и геометрия дорожек | definition, route |
| game/domain/model/GameModels.kt | Модели, статусы, инвентарь, snapshot и события | Vec2, Ball, Projectile, GameSaveState, GameEvent |
| game/data/GameSaveRepository.kt | Сохранение текущей кампании | save, restore, clear |
| game/data/GameSaveCodec.kt | Строгая сериализация цепочки | encodeBalls, decodeBalls |
| game/data/LevelProgressRepository.kt | Открытые уровни, звёзды и рекорд | recordCompletion, updateBestScore |
| game/render/GameRenderer.kt | Canvas-отрисовка всех экранов и игрового поля | draw, drawMenu, drawGame, drawEndOverlay |
| game/ui/GameView.kt | UI coordinator, frame stepping, touch routing, event side effects | onDraw, onTouchEvent, handleSystemBack |
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

1. Android вызывает GameView.onDraw().
2. View добавляет elapsed frame time в accumulator и вызывает GameEngine.update(1/60) фиксированными шагами.
3. View drains events и выполняет UI side effects: audio, progression и persistence.
4. GameView формирует GameRenderState и вызывает GameRenderer.
5. onTouchEvent преобразует screen pixels в логические координаты мира 420×900.
6. Virtual accessibility actions направляются в те же screen handlers, что и физические касания.

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

- GameEngineTest.kt: движение, столкновения/вставка, совпадения/каскады, pause/restore, progression, маршруты и бустеры.
- GameSaveCodecTest.kt: legacy round-trip, malformed tokens, blank payload и полнота цепочки.
- GameSaveRepositoryTest.kt: save/restore, upgrade неверсированной schema 0, повреждённые payload, будущая schema и защита tutorial save.
- LevelProgressRepositoryTest.kt: monotonic unlock/stars/score, валидация входа, legacy fallback и повреждённые SharedPreferences values.
- Repository tests используют Robolectric 4.17 на API 35; запуск: ./gradlew testDebugUnitTest и ./gradlew lintDebug.

## Известные точки дальнейшего рефакторинга

GameView.kt остаётся координатором с несколькими зонами ответственности. Следующий безопасный шаг — выделить FrameLoopController, GameSessionController, InputRouter, NavigationController и GameEventHandler за существующими публичными контрактами.

GameRenderer.kt пока рисует разные экраны и все игровые слои. Разделение на ScreenRenderer, TrackRenderer, HudRenderer и EffectsRenderer следует выполнить отдельным небольшим рефакторингом с golden/screenshot tests.

Тексты RU/EN пока находятся в вызовах renderer. Их перенос в Android string resources потребует отдельного UI localization pass и тестов для обоих языков.

SharedPreferences repositories покрыты Robolectric integration tests. Физическое устройство всё ещё требуется для проверки FPS, TalkBack, touch scaling, звука/вибрации и поведения при background/foreground.
