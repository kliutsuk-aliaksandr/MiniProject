# Release checklist — Marble Frog 0.5.0

## Реализовано в репозитории

- [x] Launcher icon (legacy vector и adaptive icon).
- [x] Название Marble Frog и draft store listing RU/EN.
- [x] Privacy policy в Markdown, HTML и внутри приложения.
- [x] Cloud backup и device transfer отключены.
- [x] targetSdk 36 / Android 16, minSdk 23.
- [x] R8 minification для release.
- [x] CI: debug APK, JVM tests, Android lint и unsigned release AAB.
- [x] Публичный адрес поддержки указан в privacy materials: alex.kliutsuk@gmail.com.

## Требует ручных действий разработчика / Play Console

- [ ] Добавить alex.kliutsuk@gmail.com в контактные поля Play Console и проверить получение писем.
- [ ] Включить GitHub Pages через GitHub Actions и проверить публичный HTTPS URL политики.
- [ ] Подготовить финальную иконку PNG 512×512 и feature graphic 1024×500.
- [ ] Сделать реальные скриншоты release build на поддерживаемых устройствах.
- [ ] Проверить на физических устройствах разных классов: управление, pause/resume, сохранения, масштабирование, FPS, звук, вибрация и TalkBack.
- [ ] Создать приватный upload keystore и резервную копию вне репозитория.
- [ ] Подписать AAB: CI artifact намеренно unsigned и не предназначен для прямой загрузки в Play Console.
- [ ] Заполнить App content, Data safety, content rating, target audience и ads declaration.
- [ ] Проверить требования закрытого тестирования для конкретного developer account.
- [ ] Отправить подписанную сборку на review после закрытия release gates.

## Подписание AAB

1. Скачать CI artifact miniproject-release-aab.
2. В Android Studio выбрать Build → Generate Signed Bundle / APK.
3. Выбрать Android App Bundle и создать/выбрать приватный upload key.
4. Хранить keystore и пароли вне Git и CI logs; сделать защищённую резервную копию.
5. Загрузить подписанный AAB в Play Console.

## Device QA matrix

| Сценарий | Ожидаемый результат |
|---|---|
| Restore после force-stop | Уровень, счёт, цвета, цепочка и заряды восстановлены; игра paused |
| Tutorial retry / loss | Campaign save не изменяется; остаёмся в тренировке |
| Tutorial win | Не открывает следующий campaign level и не выдаёт campaign stars |
| Bomb | Удаляет участок около последней точки прицеливания |
| Недоступный бустер | Заряд остаётся неизменным |
| Переход 8→9, 15→16, 22→23 | Активируется ожидаемый маршрут |
| RU/EN и маленький экран | Тексты и hit areas читаемы; подписи не перекрываются |
| Pause/background/resume | Время в фоне не ускоряет цепочку |
| TalkBack | Кнопки меню, карты, настроек, паузы и бустеров читаемы и доступны |

Физическое устройство и Play Console должны быть проверены вручную. Успешная сборка не заменяет эти проверки.