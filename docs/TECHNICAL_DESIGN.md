# Technical Design — Marble Frog 0.5.x

## 1. Платформа

| Параметр | Значение |
|---|---|
| Язык | Kotlin |
| Android Gradle Plugin | 8.13.2 |
| Gradle / JDK | 8.13 / 17 |
| compileSdk / targetSdk | 36 |
| minSdk | 23 |
| Application ID | com.miniproject.marbleshooter |
| versionCode / versionName | 5 / 0.5.0 |
| Графика | Android View + Canvas |
| Звук | Android ToneGenerator |
| Хранение | SharedPreferences |
| Unit-тесты | JUnit 4 + Robolectric 4.17 (API 35 test runtime) |

Проект полностью офлайн. В AndroidManifest.xml отсутствует INTERNET permission; рекламные и аналитические SDK не подключены.

## 2. Архитектура

```text
MainActivity
  └── GameView (Android View / UI coordinator)
       ├── GameLoopController (fixed-step frame timing)
       ├── ProgressPresentationCache (cached campaign presentation)
       ├── GameEngine (pure Kotlin domain aggregate)
       ├── GameRenderer (Canvas presentation)
       ├── GameSaveRepository ── GameSaveCodec
       ├── LevelProgressRepository
       ├── GameAudio
       └── GameAccessibilityNodeProvider
```

### Domain: game/domain

- GameEngine.kt — состояние партии, движение цепочки и снарядов, столкновения, вставка, комбинации, каскады, откат, бустеры, препятствия, счёт, звёзды, snapshot/restore.
- LevelCatalog.kt — декларативные параметры 30 уровней и четыре полилинии маршрута.
- domain/model/GameModels.kt — Vec2, Ball, Projectile, статусы, инвентарь бустеров, препятствия, GameSaveState и GameEvent.

Доменный слой не импортирует Android API. GameEngine является владельцем состояния симуляции; UI получает изменения через типизированные GameEvent. Структура цепочки и активных снарядов доступна внешним слоям только через unmodifiable live views; операции изменения коллекций выполняются внутри GameEngine.

### Data: game/data

- GameSaveRepository — сохранение и восстановление незавершённой кампании.
- GameSaveCodec — чистый Kotlin-кодек токенов цепочки.
- LevelProgressRepository — открытый уровень, звёзды и рекорд.

### Render: game/render

GameRenderer рисует меню, карту уровней, обучение, настройки, privacy screen, дорожку, препятствия, цепочку, снаряды, лягушку, HUD, бустеры, эффекты и overlay. Он не обрабатывает касания и не читает SharedPreferences.

### UI: game/ui

GameView преобразует координаты касаний, управляет экранами и жизненным циклом игры, обрабатывает события и создаёт GameRenderState. GameLoopController отдельно владеет accumulator, фиксированным шагом 1/60 секунды, ограничением frame delta и catch-up. ProgressPresentationCache хранит snapshot best score, unlocked level и stars; обновление репозитория происходит при изменении прогресса, а не в onDraw. GameAccessibilityNodeProvider представляет Canvas-кнопки как виртуальные узлы AccessibilityNodeProvider с названием, bounds и click action.

### Audio: game/audio

GameAudio оборачивает ToneGenerator. Сигналы генерируются локально; release() вызывается при уничтожении Activity.

## 3. Координаты и кадровый цикл

Логический мир — 420 × 900. Canvas сохраняет пропорции и центрирует поле. Ввод переводится обратным преобразованием offset/scale.

GameLoopController использует accumulator и фиксированный шаг 1/60 секунды. Время одного кадра ограничено 250 мс, catch-up — максимум 16 шагов. При pause/resume accumulator сбрасывается, поэтому время в фоне не симулируется как игровое.

GameEngine.update() дополнительно ограничивает delta до 50 мс. Отрисовка и время симуляции таким образом не зависят напрямую от колебаний частоты кадров.

## 4. Уровни и кеш маршрута

| Уровни | Тип маршрута |
|---|---|
| 1–8 | Classic |
| 9–15 | Double Loop |
| 16–22 | Switchback |
| 23–30 | Spiral |

LevelCatalog.definition(level) ограничивает ID диапазоном 1–30. При смене уровня, запуске tutorial или успешном restore метод setDefinition пересобирает routePointsCache, routeLengthCache и obstaclesCache. positionOnRoute, столкновения и renderer используют уже рассчитанные данные.

Количество шаров: min(12 + 2 × (level − 1), 70). Скорость: min(32 + 1.7 × (level − 1), 78). Количество цветов: clamp(4 + floor((level − 1)/3), 4, 6).

## 5. Формат сохранений

Файл preferences: mini_project.

| Ключ | Значение |
|---|---|
| saved_schema_version | Версия схемы; отсутствие означает legacy schema 0, актуальная схема 1 |
| saved_balls | Список colorIndex:progress через точку с запятой |
| saved_level, saved_score | Уровень и счёт |
| saved_ammo, saved_next | Текущий и следующий цвета |
| saved_aim_x, saved_aim_y | Последняя точка прицеливания |
| saved_shots | Количество выстрелов |
| booster_slow, booster_bomb, booster_color, booster_reverse | Инвентарь бустеров |

Схема 0 — прежний неверсированный формат 0.4/0.5. При успешном восстановлении репозиторий записывает schema 1. Неизвестная версия, повреждённый токен или значение SharedPreferences неожиданного типа отклоняют весь snapshot; некорректная typed value приводит к очистке snapshot вместо необработанного ClassCastException. Engine дополнительно проверяет уровень, цвета, конечность чисел, порядок цепочки, границы маршрута и количество зарядов.

Снаряды в полёте и временные эффекты не сохраняются. После восстановления партия открывается на паузе. Учебные партии исключены из persistence.

Прогресс хранится в level_progress: unlocked_level, best_score и stars_1…stars_30. Завершение уровня не уменьшает ранее сохранённые звёзды или открытый прогресс.

## 6. Бустеры и препятствия

- Slow Time: 5 секунд, множитель скорости 0.34.
- Bomb: удаляет до пяти последовательных шаров около шара, ближайшего к переданной мировой точке aimTarget. HUD touch coordinates не передаются в домен.
- Color Change: выбирает наиболее частый цвет цепочки, отличный от текущего; если выбрать нечего, действие недоступно.
- Reverse Chain: 3 секунды движения от финиша с множителем 0.65.

Применимость проверяется до consume(). Пустая цепочка не расходует Bomb или Color Change.

Препятствия применяют множитель скорости ко всей цепочке, если ведущий шар рядом с obstacle progress: Mud 0.55, Ice 1.20, Rock 0.72. Rock сейчас не физический объект, а speed modifier.

## 7. Поток событий

| Событие | Реакция UI |
|---|---|
| ShotFired | Звук выстрела |
| MatchCleared | Звук, fading marbles, particle burst, save |
| LevelStarted | Нет дополнительного side effect |
| LevelCompleted | Победа; запись звёзд/разблокировки только для кампании; очистка save |
| BoosterActivated | Click/haptic, save |
| GameLost | Звук поражения; очистка campaign save |

Учебная сессия не обновляет звёзды, unlocked level или обычный save. Повтор обучения запускает новый tutorial; tutorial win не запускает следующий campaign level.

## 8. Доступность

GameAccessibilityNodeProvider выдаёт виртуальные дочерние узлы для экранных кнопок Canvas. Каждый узел содержит локализованный label, пиксельные bounds и действие click. Набор узлов зависит от активного экрана и статуса партии.

Это улучшает навигацию TalkBack и switch access, но живое игровое поле всё ещё не имеет полного spoken-description для положения каждого шара и состояния цепочки. Нужна отдельная проверка на физическом Android-устройстве.

## 9. Тесты и CI

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew bundleRelease
```

CI выполняет debug build, JVM tests, lint и unsigned release AAB. Unit-тесты покрывают движение, collision/insertion, match/cascade, pause/restore, уровни, бустеры, кеш маршрутов, tutorial progression guard и строгий parser. JVM tests проверяют GameLoopController fixed-step/catch-up/pause behavior, а Robolectric tests — SharedPreferences save/restore, legacy schema migration, повреждённые значения, изоляцию tutorial и правила открытия уровней. Для JDK 17 тестовый runtime закреплён на API 35; это не меняет targetSdk 36 приложения.

Физическое устройство всё ещё требуется для проверки FPS, TalkBack, масштабирования координат, звука/вибрации и реальных pause/resume lifecycle callbacks. CI-сборка не заменяет эти проверки.

## 10. Приватность

Сохранения и прогресс остаются в SharedPreferences на устройстве. Cloud backup и device transfer отключены. Нет разрешения INTERNET, серверов, аккаунтов, рекламы и аналитики.