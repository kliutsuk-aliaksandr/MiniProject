# Changelog

## Unreleased — dev branch

- Цепочка на новом уровне теперь входит по одной сфере из начала дорожки; отрицательный progress корректно отображается и сохраняется до входа на экран.
- Вставка шара получила отдельную impact-анимацию: обе стороны змейки плавно расходятся, входящий шар занимает промежуток; матч и каскады разрешаются после посадки.
- Ammo и next ammo выбираются только из цветов активной цепочки и пересчитываются после изменения цепочки, swap и restore.
- Canvas presentation обновлена: мягкие фоновые glow-слои, объёмные кнопки/панели, световые акценты шаров и сгруппированный HUD.
- Начат Этап 7: OfflineProgressRepository, 8 локальных достижений и экран статистики/достижений RU/EN; tutorial/practice не засчитывается в lifetime progress.
- Добавлены регрессионные проверки появления цепочки, impact insertion, live shot palette, offline counters/achievements и изоляции tutorial.
- Bitmask достижений по бустерам зафиксирован явными стабильными битами; он не зависит от порядка значений BoosterType.
- CI feature snapshot `14cccb21aa9a280c52d52f7f393bb6993f7ccf68` успешно прошёл в [PR run 43031140238](https://github.com/kliutsuk-aliaksandr/MiniProject/actions/runs/43031140238): APK, 69 JVM tests, lint, unsigned AAB, Windows Wrapper и instrumentation API 23/29/36.
- Git workflow упорядочен как feature/* → dev → main; открыт Draft PR #3 feature → dev, устаревший PR #2 fix → main закрыт без merge.
- Подготовлена инструкция для repository Branch Ruleset `Protect main and dev` с пятью реальными required checks; применение ожидает ручного действия в GitHub Settings, поскольку подключённый коннектор имеет только read-only доступ к Rulesets.
- CI настроен на push в `main/dev` и pull_request в `main/dev`; feature PR получает один набор проверок без повторной push matrix.


- Перенесено 97 RU/EN UI-текстов в Android string resources; GameTextResources кэширует locale-specific Resources и разделяется renderer/navigation. Добавлены два теста language selection и resource-key parity.

- Добавлен стандартный Gradle Wrapper 8.13: официальный JAR с проверенным SHA-256, checksum дистрибутива в properties, CI использует ./gradlew. Добавлена Windows smoke-проверка gradlew.bat.
- Разделён GameRenderer: façade, MenuScreenRenderer, GameplayRenderer и GameRenderResources с общими Paint/Path; gameplay coordinator делегирует track/entities/effects/HUD/overlay пяти отдельным renderers. Добавлен Robolectric smoke test для всех экранов.
- Исправлен haptic feedback бустера: теперь он учитывает настройку вибрации; добавлены два controller regression tests.
- Добавлен системный аудит docs/REFACTOR_AUDIT.md с приоритетным списком дальнейших улучшений.
- Завершён рефакторинг GameView: session/event/save/progress/audio/effects перенесены в GameSessionController, экраны/hit areas/accessibility targets — в GameNavigationController, жесты — в platform-independent GameInputRouter. GameView сокращён с 712 до 145 строк и оставлен Android View/Canvas adapter.
- Добавлены 3 JVM regression tests для drag-to-aim/fire-on-release, booster gesture isolation и cancellation в GameInputRouter.
- Fixed startup crash when configuring immersive system bars before the Window DecorView existed; immersive mode is now applied after content setup and restored when the Activity regains focus.
- Added a Robolectric API 35 regression test for MainActivity startup and content/decor initialization.
- ProgressPresentationCache extracted from GameView to isolate cached campaign presentation data and avoid preference reads during rendering.
- Fixed-step frame timing extracted from GameView into independently tested GameLoopController.
- Typed SharedPreferences corruption is now rejected and cleared without leaking ClassCastException through save detection or restore.
- Публичные коллекции balls/projectiles в GameEngine заменены unmodifiable live views; структура игровых сущностей меняется только внутри симуляции.
- Прогресс кампании теперь отклоняет некорректные level/stars/score вместо принудительной нормализации, способной открыть последний уровень.
- Добавлены Robolectric repository tests для SharedPreferences save/restore, legacy schema migration, повреждённых данных, tutorial isolation и monotonic progression.
- Исправлен Bomb target: используется последняя мировая точка прицеливания, а не координата HUD.
- Повтор, победа и поражение в обучении изолированы от campaign save и прогресса; tutorial не переходит на следующий обычный уровень.
- Применимость бустера проверяется до списания заряда.
- Обновление clearing effects переведено с повторного indexOf на линейный проход.
- Добавлены schema version 1 и строгий all-or-nothing parser; прежние неверсированные сохранения считаются schema 0.
- Маршруты, их длина и препятствия кешируются на активный уровень.
- Данные прогресса кешируются вне onDraw.
- Симуляция переведена на фиксированный шаг 60 Hz с ограниченным catch-up.
- Устранено наложение подсказки тренировки с панелью бустеров; переполненный текст помечается многоточием.
- Добавлены виртуальные accessibility nodes для Canvas-кнопок.
- Обновлены immersive system bars и Android Back callback.
- Добавлены regression tests и Android lint в CI.
- Синхронизированы README, game/technical design, privacy policy, release checklist и roadmap.

## 0.5.0 — рефакторинг и новые игровые системы

- Слои domain, data, render, ui и audio.
- 30 уровней, карта, локальные звёзды и разблокировка.
- Отдельное обучение и тренировочная партия.
- Бустеры Slow Time, Bomb, Color Change и Reverse Chain.
- Маршруты Classic, Double Loop, Switchback и Spiral; препятствия Mud, Ice, Rock.
- Репозитории сохранения и прогресса; совместимость с прежними данными.

## 0.4.0 — анимация совпадения и подготовка к публикации

- Исчезновение удаляемых шаров и откат передней части цепочки.
- Каскадные совпадения после завершения анимации.
- Иконка приложения, privacy policy и unsigned AAB в CI.

## 0.3.0 — меню и управление

- Главное меню, настройки, RU/EN, переключатель звука и drag-to-aim.

## 0.2.0 — качество игрового процесса

- Столкновения и вставка шаров, каскадные комбинации, частицы, звук, пауза и сохранение.

## 0.1.0 — первый прототип

- Android Canvas marble shooter, лягушка-пушка, цепочка, счёт и основные состояния игры.
