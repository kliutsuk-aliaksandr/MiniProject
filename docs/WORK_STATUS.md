# Технический аудит — текущее состояние Marble Frog

**Снимок:** 10 ноября 2026 года  
**Рабочая ветка:** fix  
**Production refactor commit:** c6342c2372056ad5ab329fac66daae79c1c2f6eb  
**База:** main = 20ce0d75928c3988628de93a784683a411307ff2

Этот документ фиксирует состояние кода и проверок. Успешный CI подтверждает сборку и автоматические тесты, но не заменяет проверку на физическом устройстве.

## 1. Git и Pull Request

| Объект | Состояние |
|---|---|
| main | не изменялся; SHA 20ce0d75928c3988628de93a784683a411307ff2 |
| fix на production refactor commit | c6342c2372056ad5ab329fac66daae79c1c2f6eb |
| Сравнение с main на этом commit | впереди на 22 commit, позади на 0 |
| PR №2 | открыт, Draft, fix → main |
| Merge / force push | не выполнялись |

История ветки сохранялась fast-forward коммитами. PR остаётся Draft до завершения остальных аудитов. Merge не выполнять без отдельного подтверждения.

PR: https://github.com/kliutsuk-aliaksandr/MiniProject/pull/2

## 2. Выполнено

### Функциональность 0.5.0
- 30 уровней, карта, разблокировка и звёзды.
- Отдельное обучение и тренировочная партия.
- Бустеры Slow Time, Bomb, Color Change и Reverse Chain.
- Маршруты Classic, Double Loop, Switchback и Spiral; препятствия Mud, Ice и Rock.
- Локальное сохранение партии и прогресса.

### Технический hardening в fix
- Исправлена мировая цель Bomb и изоляция tutorial от campaign save/progression.
- Бустер не списывает заряд, если действие неприменимо.
- Добавлены строгий GameSaveCodec, schema version 1 и migration для legacy schema 0.
- Повреждённый payload, будущая схема и неверные типы SharedPreferences безопасно отклоняются.
- LevelProgressRepository отклоняет invalid level/stars/score и сохраняет монотонность unlock/stars/score.
- Route geometry и progression presentation кешируются.
- Публичные коллекции GameEngine balls/projectiles закрыты unmodifiable live views.
- Fixed-step loop выделен в GameLoopController: 60 Hz, frame delta clamp, bounded catch-up.
- Добавлены виртуальные accessibility nodes для Canvas controls.
- Исправлен crash при старте Activity: immersive mode настраивается после установки content view и повторно применяется при возврате фокуса.
- Добавлены regression tests и Android lint в CI.

### Завершённый рефакторинг GameView
- GameView.kt уменьшен с **712 до 145 строк**. Теперь это Android View adapter: lifecycle, Canvas rendering, coordinate transform и делегирование callbacks.
- GameSessionController.kt (306 строк) владеет GameEngine session lifecycle, GameLoopController, save/progress repositories, audio, GameEvent side effects и transient effects.
- GameNavigationController.kt (372 строки) владеет GameScreen/tutorial page, hit areas, переходами, booster/menu/settings actions и virtual accessibility targets.
- GameInputRouter.kt (68 строк) обрабатывает DOWN/MOVE/UP/CANCEL и маршрутизирует aim/fire/booster/screen tap без Android-зависимостей.
- ProgressPresentationCache.kt остаётся владельцем cached progression snapshot.
- GameRenderer не менял контракт и продолжает получать GameRenderState.

## 3. Автоматические тесты

В текущем test source — **55 JVM-тестов в 8 файлах**:

| Test class | Тестов |
|---|---:|
| GameEngineTest | 28 |
| GameSaveCodecTest | 4 |
| GameSaveRepositoryTest (Robolectric) | 7 |
| LevelProgressRepositoryTest (Robolectric) | 6 |
| GameLoopControllerTest | 3 |
| ProgressPresentationCacheTest | 3 |
| MainActivityTest (Robolectric) | 1 |
| GameInputRouterTest | 3 |
| **Итого** | **55** |

GameInputRouter tests проверяют drag-to-aim, fire only on release, изоляцию booster gesture и отмену pointer gesture. MainActivityTest фиксирует регрессию startup DecorView.

Robolectric repository/Activity tests используют API 35 test runtime. Настройки приложения остаются minSdk 23 / targetSdk 36.

## 4. Размер ключевых файлов

| Файл | Строк |
|---|---:|
| GameView.kt | 145 |
| GameSessionController.kt | 306 |
| GameNavigationController.kt | 372 |
| GameInputRouter.kt | 68 |
| GameRenderer.kt | 752 |
| GameLoopController.kt | отдельный класс |
| ProgressPresentationCache.kt | отдельный класс |

Рефакторинг выполнен по зонам ответственности, без изменения контракта GameRenderer и без переноса Android framework в domain layer.

## 5. CI и проверка устройства

На production refactor commit c6342c2372056ad5ab329fac66daae79c1c2f6eb push workflow завершился success:

- push CI: https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42842083727 — assembleDebug, testDebugUnitTest, lintDebug, bundleRelease и uploads успешны.
- PR CI: https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42842100543 — success: assembleDebug, testDebugUnitTest, lintDebug, bundleRelease и uploads завершились успешно.

Не выполнены физические проверки на реальном Android-устройстве: запуск после immersive-mode fix, touch mapping, TalkBack, background/foreground lifecycle, звук/вибрация, FPS и frame time. CI и Robolectric не подтверждают эти сценарии на Xiaomi/MIUI.

## 6. Что осталось

- [ ] Разбить GameRenderer на screen, track, HUD, effects и overlay renderers; сохранить общий draw contract и добавить доступные screenshot/golden tests.
- [ ] Перенести весь пользовательский текст RU/EN и accessibility labels в Android string resources.
- [ ] Добавить полноценные UI/instrumentation tests для level start, tutorial, pause/resume, win/loss, settings и accessibility actions.
- [ ] Выполнить physical-device проверку lifecycle, touch, TalkBack, sound/haptics и производительности.
- [ ] Проверить minSdk 23 и API 36 на эмуляторах/устройствах.
- [ ] Завершить performance allocation audit и измерить frame time на слабом устройстве.
- [ ] Подготовить screenshots/feature graphic, проверить HTTPS privacy URL, подписать AAB и завершить Play Console release gates.

## 7. Финальные команды

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew bundleRelease
```

Текущий PR остаётся Draft; main не менялся.
