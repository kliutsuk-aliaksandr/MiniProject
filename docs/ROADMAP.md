# Roadmap — Marble Frog

Статусы описывают текущее состояние исходного кода. Проверки на устройстве и в Play Console не отмечаются готовыми по факту одной CI-сборки. Технические риски, точки роста, оптимизации и приоритетный план работ собраны в [PROJECT_AUDIT.md](PROJECT_AUDIT.md).

## Этап 1 — прототип (0.1.0) — завершён

- [x] Android-проект на Kotlin.
- [x] Canvas, портретное поле, цепочка и лягушка-пушка.
- [x] Вставка и удаление групп, счёт, победа и поражение.
- [x] Первые JVM-тесты и GitHub Actions.

## Этап 2 — качество игрового процесса (0.2.0) — завершён

- [x] Вставка с обеих сторон столкновения и проверка непрерывного пути снаряда.
- [x] Каскады после схлопывания цепочки.
- [x] Частицы, звуки, пауза, продолжение и сохранение партии.

## Этап 3 — меню и управление (0.3.0) — завершён

- [x] Главное меню и настройки.
- [x] RU/EN интерфейс.
- [x] Drag-to-aim и выстрел при отпускании.
- [x] Контекстная системная Back-навигация.

## Этап 4 — подготовка к публикации (0.4.0) — код готов, release gates открыты

- [x] Иконка, карточка Google Play и privacy policy.
- [x] Отключены cloud backup и device transfer.
- [x] targetSdk 36, minSdk 23 и R8 release.
- [x] CI собирает debug APK и unsigned AAB.
- [ ] Проверить публичный HTTPS URL политики.
- [ ] Подготовить реальные скриншоты и feature graphic.
- [ ] Подписать AAB приватным upload key.
- [ ] Заполнить Play Console Data safety, content rating и target audience.
- [ ] Провести требуемое закрытое тестирование и отправить релиз на review.

## Этап 5 — контент и архитектура (0.5.0) — завершён

- [x] Domain, data, render, ui и audio слои.
- [x] Отдельное обучение и тренировочная партия.
- [x] 30 уровней, карта, разблокировки и локальные звёзды.
- [x] Четыре бустера и четыре семейства дорог с индивидуальной геометрией для всех 30 уровней.
- [x] Препятствия Mud, Ice и Rock.
- [x] Миграция прежних сохранений.

## Релиз 0.5.1 — P1 release hardening (реализовано в dev)

- [x] Обновить R8 до 9.1.29 через официальный AGP override для Kotlin 2.4.21; CI проверяет minified release на metadata warnings.
- [x] Исправить перезапуск tutorial после паузы; добавить regression test навигационного сценария.
- [x] Формировать JaCoCo XML/HTML и публиковать отчёт покрытия как CI artifact.
- [x] Установить versionName 0.5.1 и versionCode 6.
- [x] Добавить root/nested AGENTS.md и GitHub Copilot instructions.

## Этап 6 — технический hardening (ветка dev)

- [x] Исправить Bomb target: aim point вместо HUD touch point.
- [x] Изолировать tutorial от campaign save/progression; запретить переход в следующий уровень.
- [x] Проверять применимость бустера до списания заряда.
- [x] Заменить O(n²) обновление clearing effects на линейное.
- [x] Добавить строгий parser save snapshot и schema marker с legacy schema 0.
- [x] Кешировать индивидуальную геометрию дороги, длину маршрута и obstacle projections по ID активного уровня.
- [x] Кешировать snapshot прогресса вне onDraw.
- [x] Использовать fixed 60 Hz simulation step с ограниченным catch-up.
- [x] Развести tutorial hint и подписи бустеров; добавить ellipsis для переполненного текста.
- [x] Добавить виртуальные accessibility nodes для Canvas-кнопок.
- [x] Перевести immersive mode и Back на современные Android API с fallback.
- [x] Добавить regression tests и lint в CI.
- [x] Закрыть прямую структурную мутацию balls/projectiles снаружи GameEngine.
- [x] Синхронизировать README, game/technical docs, privacy и release checklist.
- [x] Извлечь fixed-step loop из GameView в GameLoopController и покрыть независимыми JVM-тестами.
- [x] Извлечь ProgressPresentationCache из GameView и покрыть snapshot/update logic unit tests.
- [x] Разделить GameView на GameSessionController (сессия, события, сохранения/прогресс, эффекты), GameNavigationController (экраны, переходы, hit areas и accessibility targets) и GameInputRouter (pointer gestures). GameView оставлен тонким Android View adapter; добавлены JVM-тесты роутинга ввода.
- [x] Разделить GameRenderer на façade, MenuScreenRenderer и GameplayRenderer; общие Paint/Path и Canvas primitives вынести в GameRenderResources.
- [x] Разделить gameplay rendering на TrackRenderer, GameEntitiesRenderer, GameEffectsRenderer, HudRenderer и GameOverlayRenderer.
- [x] Добавить Robolectric smoke test для отрисовки всех верхнеуровневых экранов.
- [x] Учитывать настройку вибрации при haptic feedback бустера; добавить regression tests для on/off.
- [x] Перенести весь UI copy из RU/EN пар в Android string resources; добавить RU/EN key parity и language-selection tests.
- [x] Добавить Robolectric tests для SharedPreferences repositories: save/restore, schema migration, corrupted payload/typed values и progression unlock rules.
- [x] Добавить Android instrumentation/UIAutomator: accessibility tree/actions и внешние touch-сценарии для меню, настроек, privacy, карты, tutorial и достижений; CI API 23/29/36.
- [ ] Ручная проверка FPS, TalkBack/switch access, gameplay hit areas, масштабирования, lifecycle, аудио/вибрации на физических устройствах.

## Этап 7 — дополнительный офлайн-контент (в работе; статистика и испытания готовы)

- [x] Локальная lifetime statistics и 8 достижений: выстрелы, комбинации, очищенные шары, best combo, звёзды, рекордные уровни и использование всех бустеров; хранение отдельно от campaign save, tutorial не засчитывается.
- [x] Три локальных испытания: лимит выстрелов, таймер+лимит выстрелов и экспертное задание; звёзды за эффективность/время и локальные рекорды хранятся отдельно от кампании.
- [ ] Дополнительные цели и награды.
- [ ] Балансировка всех 30 уровней по результатам playtesting.

## Этап 8 — после завершения офлайн-версии

- [ ] Онлайн-таблица рекордов.
- [ ] Реклама с управлением частотой и отключением.
- [ ] Покупки внутри приложения.
- [ ] Серверные функции и аналитика после отдельного privacy/security review.