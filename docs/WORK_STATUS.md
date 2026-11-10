# Технический аудит — текущее состояние Marble Frog

**Снимок состояния:** 10 ноября 2026 года  
**Рабочая ветка:** `fix`  
**Кодовый срез ветки fix:** `c762f3759134b12199c49da0518ce7850fbea0c3` (17 коммитов впереди main; 0 позади)

Этот документ фиксирует фактическое состояние исходного кода и GitHub CI. Отметка «сделано» означает, что изменение уже есть в ветке; она не означает, что каждое поведение проверено на физическом устройстве.

## 1. Git и Pull Request

| Объект | Текущее состояние |
|---|---|
| `main` | `20ce0d75928c3988628de93a784683a411307ff2` |
| `fix` (кодовый срез этого отчёта) | `c762f3759134b12199c49da0518ce7850fbea0c3` |
| PR №2 | открыт, Draft, направление `fix → main` |
| Сравнение веток | `fix` впереди на 17 коммитов, отставание — 0 |
| Merge | не выполнялся и не должен выполняться без отдельного подтверждения |

Удалённые ветки `main`, `fix` и feature-ветка сохранены. История не переписывалась, force push не применялся.

PR: [Fix: harden Marble Frog gameplay, saves and accessibility](https://github.com/kliutsuk-aliaksandr/MiniProject/pull/2)

## 2. Что уже сделано

### 2.1. Функциональность версии 0.5.0 в main

В `main` находится объединённая версия 0.5.0:

- 30 уровней и карта прогресса со звёздами и разблокировкой;
- четыре типа маршрутов: Classic, Double Loop, Switchback и Spiral;
- препятствия Mud, Ice и Rock;
- четыре бустера;
- отдельное обучение и тренировочная партия;
- локальное сохранение партии и прогресса;
- разделение исходников на domain, data, render, ui и audio.

### 2.2. Технические исправления в fix

- Исправлена цель Bomb: используется мировая точка прицеливания.
- Обучение изолировано от кампании: тренировочная партия не перезаписывает сохранение, рекорд или progression; tutorial не переводит на следующий campaign level.
- Бустер списывает заряд только после проверки применимости действия.
- Обновление clearing effects оптимизировано до линейного прохода.
- Добавлены строгий GameSaveCodec, schema version 1 и совместимость с legacy schema 0.
- Повреждённый payload, неизвестная схема и неверные типы значений SharedPreferences отклоняются; некорректный snapshot очищается без необработанного ClassCastException.
- LevelProgressRepository отклоняет недопустимые level/stars/score вместо принудительного ограничения ID, которое могло открыть последний уровень.
- Кешируются геометрия маршрута, длина маршрута и проекции препятствий.
- Публичные коллекции `GameEngine.balls` и `projectiles` доступны как unmodifiable live views; внешняя структурная мутация запрещена.
- Fixed-step loop вынесен из GameView в `GameLoopController`: 60 Hz, ограничение frame delta и ограниченный catch-up.
- Кеш представления прогресса вынесен в `ProgressPresentationCache`; preference reads не выполняются на каждом кадре.
- Добавлены виртуальные accessibility nodes для Canvas-кнопок.
- Обновлены Back navigation и immersive/system UI с fallback для старых API.
- Обновлены README и техническая документация; добавлен CODE_REFERENCE.

### 2.3. Тесты

В текущем дереве — **51 JVM-тест в 6 test-файлах**:

| Test class | Количество |
|---|---:|
| GameEngineTest | 28 |
| GameSaveCodecTest | 4 |
| GameSaveRepositoryTest (Robolectric) | 7 |
| LevelProgressRepositoryTest (Robolectric) | 6 |
| GameLoopControllerTest | 3 |
| ProgressPresentationCacheTest | 3 |
| **Всего** | **51** |

Robolectric repository tests запускаются на test runtime API 35. Это не меняет app minSdk 23 и targetSdk 36.

## 3. Что ещё не сделано

### Высокий приоритет — архитектура и качество

- [ ] Разделить оставшиеся обязанности GameView: GameSessionController, InputRouter, NavigationController и GameEventHandler.
- [ ] Разделить GameRenderer на ScreenRenderer, TrackRenderer, HudRenderer, EffectsRenderer и OverlayRenderer. Разделение должно идти по ответственности, не механическим переносом методов.
- [ ] Перенести пользовательские строки из Kotlin в Android resources: `res/values/strings.xml` и `res/values-en/strings.xml`. Сейчас `strings.xml` содержит только app_name; пары RU/EN и accessibility labels в основном заданы в коде.
- [ ] Завершить allocation audit hot path. В GameRenderer ещё есть повторные RectF/list/Triple и другие временные объекты; необходимо измерить реальные allocations, а не добавлять кеши без профилирования.

### Высокий приоритет — проверки

- [ ] Добавить UI/instrumentation tests ключевых сценариев: запуск уровня, tutorial, pause/resume, победа/поражение, settings/language и accessibility actions.
- [ ] Добавить автоматические accessibility checks там, где это поддерживается тестовым стеком; отдельно проверить семантику виртуальных узлов.
- [ ] Проверить lifecycle/background/foreground, touch coordinate mapping, TalkBack, звук и вибрацию на Android-устройствах.
- [ ] Проверить совместимость на реальных/эмуляторных API 23, промежуточных версиях и API 36.
- [ ] Измерить FPS/frame time и allocations на слабом устройстве; проверить отсутствие деградации после рефакторинга.

### Средний приоритет — документация и релиз

- [ ] После каждого архитектурного этапа синхронизировать CODE_REFERENCE и TECHNICAL_DESIGN с фактическим кодом.
- [ ] Подготовить реальные screenshots и feature graphic.
- [ ] Проверить публичный HTTPS URL privacy policy.
- [ ] Подписать release AAB приватным upload key.
- [ ] Заполнить Play Console Data safety, content rating и target audience, пройти требуемое тестирование и review.

Онлайн-функции, реклама, покупки и серверная аналитика не входят в текущий offline hardening и остаются отдельным будущим этапом.

## 4. Размер ключевых файлов сейчас

| Файл | Строк | Состояние |
|---|---:|---|
| `GameView.kt` | 712 | Часть ответственности уже вынесена, но класс всё ещё крупный |
| `GameRenderer.kt` | 752 | Разделение на renderer-компоненты ещё не выполнено |
| `GameLoopController.kt` | отдельный класс | Выделен и покрыт 3 JVM-тестами |
| `ProgressPresentationCache.kt` | отдельный класс | Выделен и покрыт 3 JVM-тестами |

Текущие размеры не являются самостоятельной метрикой качества; важнее уменьшить связанность и обеспечить тестируемые контракты.

## 5. CI: что именно подтверждено

Последний полностью зелёный PR workflow до последнего исправления GameView был на commit `6e751f7165a73224fa64609ddc438d2ed79c8667`:

- [GitHub Actions run 42834116310](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42834116310) — success;
- [GitHub Actions run 42834106713](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42834106713) — success.

На commit `8f0061c4462f0f749e481a50bef1d482841a94d0` CI обнаружил compile error: при извлечении ProgressPresentationCache из GameView было удалено поле `preferences`, хотя настройки языка, звука и вибрации продолжали его использовать. Это исправлено отдельным commit `bda872de2c2a429cc2d3e6e0708f46f4cb8bb5ac`.

На commit `bda872de2c2a429cc2d3e6e0708f46f4cb8bb5ac` workflow [42838489410](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42838489410) уже прошёл `assembleDebug`, `testDebugUnitTest` и `lintDebug`; на последней проверке продолжал выполняться `bundleRelease`.

**Проверка кодового среза `c762f3759134b12199c49da0518ce7850fbea0c3`:** workflow [42838762914](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/42838762914) выполнялся; подготовка runner/SDK/Gradle завершилась, начался `assembleDebug`. Полный успех этого workflow ещё не подтверждён. Перед дальнейшим рефакторингом необходимо проверить именно этот кодовый срез; если workflow красный — сначала исправить причину.

Предыдущие зелёные результаты не заменяют проверку более нового commit.

## 6. Порядок дальнейших действий

1. **Закрыть текущий CI gate.** Дождаться workflow для кодового среза `c762f375...`; проверить assembleDebug, testDebugUnitTest, lintDebug и bundleRelease. Если упадёт — исправить причину, повторить полный CI.
2. **Довести GameView до управляемого coordinator.** Сначала выделить event handling и session lifecycle, затем input routing и navigation. Для каждого выделения добавить тесты и сравнить поведение до/после.
3. **Разделить GameRenderer.** Начать с TrackRenderer и EffectsRenderer, затем HUD и overlays, последним — маршрутизацию экранов. Сохранить общий draw contract и добавить screenshot/golden или доступные Robolectric проверки.
4. **Вынести локализацию.** Создать полный RU и EN набор resources, включая tutorial, HUD, boosters, pause/end screens, privacy и accessibility labels. Проверить полноту обоих наборов автоматически.
5. **Добавить UI и accessibility tests.** Покрыть ключевые end-to-end сценарии и проверить, что виртуальные Canvas targets имеют правильные labels, bounds и click actions.
6. **Закончить performance audit.** Убрать подтверждённые временные allocations из горячих участков, проверить геометрию и particle updates, измерить frame time/allocations на эмуляторе и физическом устройстве.
7. **Закончить Android compatibility pass.** Проверить minSdk 23, Back gesture, WindowInsets, lifecycle callbacks, TalkBack и audio release/resume.
8. **Сделать финальный release review.** Обновить документацию, выполнить все четыре Gradle-команды, проверить последний GitHub Actions workflow и release checklist. Только после этого предложить снять Draft; merge — исключительно по отдельному подтверждению.

## 7. Обязательные команды финальной проверки

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew bundleRelease
```

Статус CI должен быть привязан к тому же commit SHA, который указан в HEAD PR. Успешная сборка не считается заменой физическому тестированию TalkBack, touch, аудио, lifecycle и frame performance.
