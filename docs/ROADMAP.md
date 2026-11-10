# Roadmap — Marble Frog

Статусы описывают текущее состояние исходного кода. Проверки на устройстве и в Play Console не отмечаются готовыми по факту одной CI-сборки.

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
- [x] Четыре бустера и четыре маршрута.
- [x] Препятствия Mud, Ice и Rock.
- [x] Миграция прежних сохранений.

## Этап 6 — технический hardening (ветка fix)

- [x] Исправить Bomb target: aim point вместо HUD touch point.
- [x] Изолировать tutorial от campaign save/progression; запретить переход в следующий уровень.
- [x] Проверять применимость бустера до списания заряда.
- [x] Заменить O(n²) обновление clearing effects на линейное.
- [x] Добавить строгий parser save snapshot и schema marker с legacy schema 0.
- [x] Кешировать маршрут, длину маршрута и obstacle projections на уровне.
- [x] Кешировать snapshot прогресса вне onDraw.
- [x] Использовать fixed 60 Hz simulation step с ограниченным catch-up.
- [x] Развести tutorial hint и подписи бустеров; добавить ellipsis для переполненного текста.
- [x] Добавить виртуальные accessibility nodes для Canvas-кнопок.
- [x] Перевести immersive mode и Back на современные Android API с fallback.
- [x] Добавить regression tests и lint в CI.
- [x] Синхронизировать README, game/technical docs, privacy и release checklist.
- [ ] Разбить GameView на loop/session/input/navigation/event controllers.
- [ ] Разбить GameRenderer на отдельные screen/field/HUD/effects renderers.
- [ ] Перенести весь UI copy из RU/EN пар в Android string resources.
- [x] Добавить Robolectric tests для SharedPreferences repositories: save/restore, schema migration, corrupted values и progression unlock rules.
- [ ] Добавить instrumented UI tests и automated accessibility checks.
- [ ] Проверить FPS, TalkBack, масштабирование и touch на физических устройствах.

## Этап 7 — дополнительный офлайн-контент

- [ ] Локальные достижения и статистика.
- [ ] Испытания с ограничением времени/выстрелов.
- [ ] Дополнительные цели и награды.
- [ ] Балансировка всех 30 уровней по результатам playtesting.

## Этап 8 — после завершения офлайн-версии

- [ ] Онлайн-таблица рекордов.
- [ ] Реклама с управлением частотой и отключением.
- [ ] Покупки внутри приложения.
- [ ] Серверные функции и аналитика после отдельного privacy/security review.