# Changelog

## Unreleased — fix branch

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
