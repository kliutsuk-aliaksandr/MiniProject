# Marble Frog

Marble Frog — офлайн-игра в жанре marble shooter для Android. Игрок направляет лягушку-пушку, вставляет цветные шары в движущуюся цепочку и удаляет группы из трёх и более соседних шаров одного цвета.

## Текущая версия

**0.5.0** (versionCode 5, targetSdk 36, minSdk 23). Дальнейшая разработка и техническое укрепление ведутся в ветке dev; main сохраняет стабильную линию.

## Возможности

- 30 локальных уровней с постепенным ростом длины цепочки, скорости и числа цветов.
- Четыре маршрута: Classic (1–8), Double Loop (9–15), Switchback (16–22), Spiral (23–30).
- Препятствия Mud, Ice и Rock изменяют скорость движения цепочки.
- Четыре бустера: Slow Time, Bomb, Color Change и Reverse Chain.
- Отдельный обучающий экран и тренировочная партия.
- Карта уровней, разблокировка следующих этапов и локальные звёзды (1–3).
- Сохранение незавершённой кампании и прогресса на устройстве.
- Русский и английский интерфейс, настройки звука и вибрации.
- Полностью офлайн: нет аккаунта, рекламы, аналитики и разрешения INTERNET.

## Игровой цикл и управление

1. Цепочка движется по маршруту к финишу.
2. Веди пальцем по полю, чтобы прицелиться; отпусти палец, чтобы выстрелить.
3. При столкновении шар вставляется с передней или задней стороны ближайшего шара с учётом направления столкновения.
4. Три и более соседних шара одного цвета удаляются; после схлопывания проверяются каскадные комбинации.
5. Очисти всю цепочку, чтобы завершить уровень. Если ведущий шар достигнет финиша, раунд проигран.

NEXT меняет текущий и следующий шар. Верхняя панель управляет звуком и паузой. Нижняя панель содержит бустеры. Bomb выбирает ближайший к последней точке прицеливания участок цепочки.

### Бустеры

| Бустер | Эффект |
|---|---|
| Slow Time | Замедляет цепочку на 5 секунд (0,34× базовой скорости). |
| Bomb | Удаляет до пяти соседних шаров около точки прицеливания. |
| Color Change | Выбирает наиболее частый цвет цепочки, отличный от заряженного шара. |
| Reverse Chain | На 3 секунды разворачивает движение со скоростью 0,65× базовой. |

Заряд расходуется только после проверки, что бустер может выполнить действие.

### Сложность и звёзды

- Количество шаров: min(12 + 2 × (level − 1), 70).
- Скорость: min(32 + 1.7 × (level − 1), 78).
- Число цветов растёт каждые три уровня от четырёх до шести.
- Оценка в 1–3 звезды зависит от количества выстрелов относительно числа шаров в целевой цепочке.

## Технологии

- Kotlin, Android Gradle Plugin 8.13.2, Gradle 8.13, JDK 21 (bytecode target remains Java/Kotlin 17).
- Android SDK 36, Canvas API, ToneGenerator.
- SharedPreferences для локального сохранения.
- JUnit 4 и Robolectric 4.17 для JVM и SharedPreferences repository tests (тестовый runtime API 35).

## Сборка и проверка

Требуются JDK 21+, Android SDK Platform 36 и Build Tools 36.0.0.

Linux/macOS:
```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew bundleRelease
```

Windows:
```bat
gradlew.bat testDebugUnitTest
gradlew.bat lintDebug
gradlew.bat assembleDebug
gradlew.bat bundleRelease
```

CI release AAB не подписан. Перед отправкой в Google Play его необходимо подписать приватным upload key; keystore и пароли нельзя добавлять в Git.

## Архитектура и структура проекта

```text
app/src/main/java/com/miniproject/marbleshooter/
├── MainActivity.kt                         # Activity lifecycle, system UI, Back
└── game/
    ├── audio/GameAudio.kt                  # offline ToneGenerator adapter
    ├── data/
    │   ├── GameSaveRepository.kt           # current session persistence
    │   ├── GameSaveCodec.kt                # strict snapshot codec
    │   └── LevelProgressRepository.kt      # unlocks, stars, best score
    ├── domain/
    │   ├── GameEngine.kt                   # platform-independent simulation
    │   ├── LevelCatalog.kt                  # levels and route definitions
    │   └── model/GameModels.kt              # values, state and domain events
    ├── render/
    │   ├── GameRenderer.kt                  # small Canvas renderer façade
    │   ├── MenuScreenRenderer.kt             # menu, map, tutorial, settings and privacy
    │   ├── GameplayRenderer.kt                # gameplay draw coordinator
    │   ├── TrackRenderer.kt                    # route and obstacles
    │   ├── GameEntitiesRenderer.kt              # chain, projectiles and frog
    │   ├── GameEffectsRenderer.kt               # clearing and particle effects
    │   ├── HudRenderer.kt                       # score, status and boosters
    │   ├── GameOverlayRenderer.kt                # pause, win and loss overlays
    │   └── GameRenderResources.kt                # shared Paint/Path cache and primitives
    └── ui/
        ├── GameView.kt                     # Android View, Canvas and coordinate adapter
        ├── GameSessionController.kt        # gameplay lifecycle, events, saves, audio and effects
        ├── GameNavigationController.kt     # screens, transitions, hit areas and accessibility targets
        ├── GameInputRouter.kt               # pointer gesture routing without Android dependencies
        ├── GameLoopController.kt           # bounded fixed-step frame timing
        ├── ProgressPresentationCache.kt    # cached stars, unlocks and best score
        └── GameAccessibilityNodeProvider.kt # virtual Canvas accessibility buttons
```

domain не зависит от Android. data владеет SharedPreferences. render отвечает только за Canvas. ui разделяет Android View adapter, session/persistence, navigation and pointer input controllers. audio изолирует платформенные звуковые сигналы.

## Сохранения

Данные партии находятся в SharedPreferences mini_project, а прогресс — в level_progress. Версия 0 формата сохранений соответствует старым неверсированным данным; актуальная версия — 1. Парсер отклоняет всю цепочку, если некорректен хотя бы один токен. После восстановления старая версия помечается актуальной. Обучение не сохраняется и не меняет кампанию.

## Документация

- docs/TECHNICAL_DESIGN.md — архитектура, данные, игровой цикл и CI.
- docs/CODE_REFERENCE.md — описание классов, методов, событий и рекомендации по расширению.
- docs/GAME_DESIGN.md — правила, уровни, препятствия, бустеры и очки.
- docs/ROADMAP.md — завершённые этапы и технический backlog.
- docs/PROJECT_AUDIT.md — актуальный технический и продуктовый аудит, риски, оптимизации и приоритетный план развития.
- docs/RELEASE_CHECKLIST.md — оставшиеся ручные и Play Console проверки.
- docs/PRIVACY_POLICY.md — политика конфиденциальности.

## CI и приватность

GitHub Actions собирает debug APK, запускает unit-тесты и Android lint, формирует unsigned release AAB, проверяет Gradle Wrapper на Windows и выполняет UI instrumentation на API 23/29/36. Автоматический SonarQube Quality Gate с отчётом покрытия пока не включён — см. docs/PROJECT_AUDIT.md. Приложение не запрашивает доступ к интернету; сохранения остаются локальными. См. docs/PRIVACY_POLICY.md.
