# tgBot
# FlightWatcher

FlightWatcher отслеживает минимальные цены на авиабилеты по заданным маршрутам и датам. Подписки хранятся в PostgreSQL, а уведомления об изменении цены приходят пользователям в Telegram.

## Возможности

- Создание подписки через Telegram-бота: выбор маршрута, даты вылета и максимальной цены.
- Поиск цены на билет через Travelpayouts API.
- Периодическая проверка сохранённых подписок и отправка уведомлений в Telegram.
- Просмотр, редактирование и удаление подписок через Telegram.
- REST API для просмотра, редактирования и удаления подписок.
- Каталог аэропортов из CSV и поиск аэропорта по IATA-коду через API Travelpayouts.

## Технологии

- Java 21
- Spring Boot 4.1.1: Spring MVC, Spring Data JPA, Validation, Scheduling и RestClient
- PostgreSQL
- Flyway для миграций базы данных
- Maven и Maven Wrapper
- JUnit, Spring Boot Test и Testcontainers для тестов

## Как работает приложение

1. Пользователь создаёт подписку в Telegram: выбирает аэропорты вылета и прилёта, дату и лимит цены.
2. Подписка сохраняется в таблице `price_alert` в PostgreSQL вместе с ID Telegram-чата.
3. Планировщик регулярно получает цены из Travelpayouts и выбирает минимальную.
4. Если цена найдена и изменилась более чем на 200 ₽ относительно последней сохранённой цены, приложение отправляет сообщение в Telegram и сохраняет новую цену.

Если цена изменилась не более чем на 200 ₽, уведомление не отправляется. При отсутствии найденных предложений подписка остаётся активной и проверяется снова при следующем запуске планировщика.

## Внешние API

### Цены на билеты — Travelpayouts

Для запроса цен используется:

`GET https://api.travelpayouts.com/aviasales/v3/prices_for_dates`

Передаются код аэропорта вылета и прилёта, дата вылета, валюта `rub`, рынок `ru` и параметры выдачи. Для авторизации используется заголовок `X-Access-Token`.

### Поиск аэропортов — Travelpayouts

Для поиска по IATA-коду используется:

`GET https://autocomplete.travelpayouts.com/places2`

Поиск применяется при создании подписки, если введённого аэропорта нет в локальном каталоге.

### Telegram Bot API

Приложение использует Telegram Bot API для:

- получения сообщений и нажатий на кнопки методом `getUpdates` (опрос выполняется каждые 3 секунды);
- отправки сообщений и inline-клавиатур через `sendMessage`;
- обработки callback-кнопок через `answerCallbackQuery`;
- настройки списка команд через `setMyCommands` и кнопки меню через `setChatMenuButton`.

## Команды Telegram-бота

- `/start`, `/help` — приветствие и справка;
- `/add` — создать подписку;
- `/get` — посмотреть подписки и открыть действия для изменения или удаления;
- `/delete` — удалить подписку;
- `/cancel` — отменить незавершённое создание или изменение.

## REST API

Базовый адрес при локальном запуске: `http://localhost:8080`.

| Метод | Путь | Назначение |
|---|---|---|
| `GET` | `/api/health` | Проверить состояние приложения |
| `GET` | `/api/hello/{name}` | Проверочный endpoint, возвращает приветствие |
| `GET` | `/api/alerts` | Получить список подписок |
| `GET` | `/api/alerts/{id}` | Получить подписку по ID |
| `PATCH` | `/api/alerts/{id}` | Изменить поля подписки |
| `DELETE` | `/api/alerts/{id}` | Удалить подписку |

Для `PATCH` передавайте JSON только с изменяемыми полями, например:

```http
PATCH http://localhost:8080/api/alerts/1
Content-Type: application/json

{
  "maxPrice": 20000
}
```

Создание подписки в текущем REST-контроллере не реализовано: подписка создаётся через Telegram-бота.

## База данных

Используется PostgreSQL. По умолчанию приложение подключается к локальной базе:

- база данных: `flightwatcher`
- пользователь: `postgres`
- адрес: `localhost:5432`

При старте Flyway применяет миграции из `src/main/resources/db/migration`. Основная таблица — `price_alert`; в ней хранятся маршрут, дата вылета, максимальная цена, последняя проверенная цена и ID Telegram-чата.

Создайте базу данных перед запуском, если её ещё нет:

```sql
CREATE DATABASE flightwatcher;
```

Параметры подключения задаются в `src/main/resources/application.properties`. Не храните реальные токены и пароли в публичном репозитории.

## Настройка

Задайте переменные окружения для токенов:

PowerShell:

```powershell
$env:TRAVELPAYOUTS_TOKEN = "ваш-токен-Travelpayouts"
$env:TELEGRAM_BOT_TOKEN = "токен-бота-Telegram"
```

Настройте подключение к PostgreSQL в `application.properties` при необходимости. Плановый интервал проверки задаётся свойством `flightwatcher.check-interval-ms`; в текущей конфигурации он равен `10000` миллисекундам.

## Запуск в Windows

Убедитесь, что PostgreSQL запущен, база `flightwatcher` создана, а переменные окружения с токенами заданы.

### Запуск собранного JAR из `target`

```powershell
java -jar .\target\flightwatcher-0.0.1-SNAPSHOT.jar
```

### Сборка и запуск через Maven Wrapper

```powershell
.\mvnw.cmd clean package
java -jar .\target\flightwatcher-0.0.1-SNAPSHOT.jar
```

Для запуска приложения через Maven без отдельного запуска JAR:

```powershell
.\mvnw.cmd spring-boot:run
```

## Основные компоненты

- `AlertController` и `AlertService` — REST-операции с подписками.
- `PriceAlertRepository` и `PriceAlert` — доступ к сущности подписки через JPA.
- `FlightPriceScheduler` и `AlertCheckService` — периодическая проверка подписок.
- `FlightPriceService` и `FlightPriceClient` — получение предложений и выбор минимальной цены.
- `AirportCatalogService` и `AirportLookupClient` — локальный каталог и внешний поиск аэропортов.
- `TelegramPollingService` — получение и маршрутизация обновлений Telegram.
- `TelegramNotificationService` — отправка сообщений и настройка меню бота.
- `TelegramAddSubscriptionService`, `TelegramGetSubscriptionService`, `TelegramEditSubscriptionService`, `TelegramDeleteSubscriptionService` — диалоги управления подписками в Telegram.

## Тесты

Запустить тесты через Maven Wrapper:

```powershell
.\mvnw.cmd test
```

Для интеграционных тестов с Testcontainers потребуется доступный Docker.

