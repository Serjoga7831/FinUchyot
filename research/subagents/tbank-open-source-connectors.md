## Итог

Нашёл два существенно разных коннектора ZenMoney в публичном репозитории **[zenmoney/ZenPlugins](https://github.com/zenmoney/ZenPlugins)**:

1. **Тинькофф для физлиц** — исторический, неактуальный reverse-engineered коннектор к приватному mobile/web API. Он был **удалён 2 марта 2020** коммитом `1efdd11c` с явной причиной: *“drop deprecated and non-working plugins”*. Для нового FinUchyot его нельзя считать рабочей основой.
2. **Тинькофф Бизнес / T‑Business** — текущий коннектор, использующий OAuth authorization-code + refresh-token и business API счетов/выписок. Ветка `master` поддерживается: последний коммит коннектора — **4 августа 2026**, обновлял доверенный TLS-сертификат. Это наиболее релевантный референс, но он для бизнес-счетов, не для розничных продуктов физлица.

## 1) Исторический личный Тинькофф: что делал

**Код на фиксированном последнем рабочем снимке:**
- [api.js](https://github.com/zenmoney/ZenPlugins/blob/1655890a1e9e5bfb1669a991a3f8215c5dc2743d/src/plugins/tinkoff/api.js)
- [index.js](https://github.com/zenmoney/ZenPlugins/blob/1655890a1e9e5bfb1669a991a3f8215c5dc2743d/src/plugins/tinkoff/index.js)
- [converters.js](https://github.com/zenmoney/ZenPlugins/blob/1655890a1e9e5bfb1669a991a3f8215c5dc2743d/src/plugins/tinkoff/converters.js)
- [настройки пользователя](https://github.com/zenmoney/ZenPlugins/blob/1655890a1e9e5bfb1669a991a3f8215c5dc2743d/src/plugins/tinkoff/preferences.xml)
- [коммит удаления](https://github.com/zenmoney/ZenPlugins/commit/1efdd11c671ea5f5d18cd5e4bb26a12189780c0b)

### Вход и сессия — на архитектурном уровне

Коннектор:
- создавал банковскую сессию;
- использовал логин либо номер телефона и пароль;
- обрабатывал интерактивное подтверждение входа пользователем;
- регистрировал локальный PIN для последующих входов;
- повышал уровень доступа и обрабатывал дополнительное подтверждение;
- посылал идентификатор сессии и устойчивый идентификатор устройства с каждым запросом.

Не воспроизвожу последовательность вызовов или параметры, достаточные для эксплуатации приватного API. Существенный вывод для проектирования: это **эмуляция старого клиента**, а не документированный партнёрский интерфейс; она завязана на поведение антифрода, device binding, MFA и внутренние форматы ответа.

### Локальное хранение

Через sandbox-хранилище ZenMoney (`ZenMoney.getData/setData`) сохранялись:
- стабильный `device_id`;
- PIN-хэш и время его установки;
- последний session ID;
- флаг первичной инициализации счетов.

Пароль не записывался этим кодом в persistent storage: при отсутствии в настройках он запрашивался интерактивно. Это хорошая граница для FinUchyot: секреты и refresh credentials должны быть отделены от обычной БД/логов, а сессионные артефакты — шифроваться в системном secure storage.

### Счета и операции

Один пакетный запрос запрашивал одновременно:
- плоский список счетов;
- операции начиная с даты синхронизации.

`converters.js` затем приводил ответы к общей модели ZenMoney:
- счета: дебетовые, детские, кредитные карты, накопительные, вклады/мультивалютные вклады, кредиты, виртуальные карты и др.;
- транзакции: доходы, переводы, внутренние перемещения, наличные, платежи;
- мультивалютные операции;
- `hold`-операции;
- устранение дублей и зеркальные движения при переводах;
- merchant/MCC/comment/invoice;
- исключение внешних и импортированных счетов.

Тестовые фикстуры для крайних случаев (дубли, холды, типы счетов) были частью удалённого плагина и доступны по дереву коммита `1655890a`.

### Работоспособность

**Не работает как поддерживаемый вариант:** последний коммит по личному плагину — 21 февраля 2020; через 10 дней модуль удалили как deprecated/non-working. Никаких попыток логина/синхронизации не выполнял.

## 2) Т‑Бизнес: актуальный референс

**Ключевые файлы:**
- [OAuth, refresh и HTTP API](https://github.com/zenmoney/ZenPlugins/blob/master/src/plugins/tinkoff-business/api.js)
- [главный sync flow](https://github.com/zenmoney/ZenPlugins/blob/master/src/plugins/tinkoff-business/index.js)
- [маппинг счетов и операций](https://github.com/zenmoney/ZenPlugins/blob/master/src/plugins/tinkoff-business/converters.js)
- [настройки: ИНН, КПП, start date](https://github.com/zenmoney/ZenPlugins/blob/master/src/plugins/tinkoff-business/preferences.xml)
- [манифест: public API + подписка](https://github.com/zenmoney/ZenPlugins/blob/master/src/plugins/tinkoff-business/ZenmoneyManifest.xml)
- [официальный портал T‑API](https://developer.tinkoff.ru/docs/api)

### Протокол

Это нормальный OAuth-паттерн:
- открывает авторизацию T‑ID в web view;
- создаёт и проверяет `state`;
- перехватывает redirect только на сконфигурированный `redirect_uri`;
- меняет authorization code на access/refresh tokens;
- обновляет access token за пять минут до expiry;
- проверяет, что выданы scopes на чтение счетов и выписок.

Нужные бизнес-ресурсы в реализации:
- список счетов: business API v4 bank accounts;
- выписка: business API bank statement по каждому счёту и диапазону дат;
- авторизация: T‑ID token endpoint.

Это соответствует категории возможностей, опубликованной на официальном Dev Portal: «Счета и выписки» и T‑ID.

### Локальная сессия

В `ZenMoney.getData('auth')` сохраняется объект:
- access token;
- refresh token;
- `expirationDateMs`.

При `401`/`AUTH_REQUIRED` поток сначала принудительно refresh-ит credential, затем при необходимости запускает повторную авторизацию. Токены и код авторизации в коде помечены для sanitization в логах.

### Получение и нормализация данных

Главный flow:
1. Нормализует ИНН/КПП.
2. Восстанавливает или обновляет OAuth credential.
3. Читает список счетов.
4. Для каждого неотключённого пользователем счёта загружает выписку в `fromDate..toDate`.
5. Преобразует записи в внутренние transaction data, сортирует по дате и возвращает unified accounts/transactions.

### Текущая работоспособность — аккуратная оценка

- **Есть признаки живой поддержки:** файл `index.js` обновлён коммитом [65d75c0](https://github.com/zenmoney/ZenPlugins/commit/65d75c016c42ba69b7d455eaeaa263921666c3e1) 4 августа 2026 для актуального TLS-сертификата; API-слой обновлялся для v4 счетов в 2024.
- **Не подтверждал live-синхронизацию:** это требовало бы реальной бизнес-авторизации и пользовательских данных, чего задача прямо не допускает.
- В public checkout `config.json` содержит пустые client ID/secret/redirect URI — значит production-параметры инжектируются/поставляются отдельно, и этот репозиторий сам по себе не является готовым standalone-клиентом.

## Лицензирование

- **ZenPlugins — GPL-3.0-only:** [LICENSE](https://github.com/zenmoney/ZenPlugins/blob/master/LICENSE).
- Если FinUchyot копирует или существенно перерабатывает код коннектора, это создаёт GPL-обязательства для распространяемой производной работы. Безопаснее взять **архитектурные идеи и документированный OAuth/API контракт**, а реализацию написать независимо с чистой-room спецификацией.
- Отдельно найденный [`Fatal1ty/tinkoff-api`](https://github.com/Fatal1ty/tinkoff-api) имеет Apache-2.0, но это **архив с сентября 2023** и SDK **Tinkoff Invest API**, не банковские счета/карточные транзакции физлица. Он не решает задачу FinUchyot.

## Рекомендация для FinUchyot

- Для **Т‑Бизнес**: ориентироваться на официальный OAuth/T‑ID и API выписок; взять у ZenMoney дизайн refresh, retry-after-auth, scopes, раздельный storage credentials и conversion layer, но реализовать независимо.
- Для **физлиц Т‑Банка**: не строить продукт на старом private-API плагине ZenMoney — он документированно заброшен как неработающий. Нужны официальный пользовательский API/партнёрское соглашение либо другой разрешённый способ импорта (например, экспорт выписки пользователем), а не эмуляция приложения/обход защит.
- Архитектурно разделить: `Auth provider → encrypted credential store → accounts adapter → transactions adapter → idempotent normalizer/deduper`. Особенно важны stable external IDs, pending/hold semantics, валютные суммы и переводы между собственными счетами.

## Локальные артефакты

- Создан только исследовательский клон: `/home/serg/ZenPlugins-research`.
- Изменений исходного проекта или сетевых/банковских действий не выполнялось.