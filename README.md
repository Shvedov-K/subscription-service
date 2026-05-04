# Subscription Service

Сервис управления подписками с кэшированием и обработкой событий.

## Описание

**Subscription Service** - это микросервис на Spring Boot, который предоставляет REST API для управления подписками пользователей и выставления счетов. Сервис состоит из двух основных модулей:

- **Subscription Module** - основная бизнес-логика управления подписками
- **Cache Module** - кэширование данных пользователей с fallback-механизмом

## Функциональность

### **Основные возможности**

#### Управление подписками
- Активация подписок (Basic/PRO)
- Деактивация подписок
- Валидация бизнес-правил (одна активная подписка на пользователя)
- Проверка даты активации (не может быть в прошлом)

#### Выставление счетов
- Ежемесячное автоматическое выставление счетов
- Ценообразование: Basic - 100₽, PRO - 200₽
- Отправка событий в RabbitMQ
- Outbox pattern для гарантированной доставки

#### Кэширование
- Хранение данных в Redis
- Fallback к основному сервису через REST при недоступности Redis
- Пагинация кэшированных данных

## **Технологический стек**

| Компонент | Технология | Версия |
|-----------|------------|--------|
| **Framework** | Spring Boot | 4.0.6 |
| **Language** | Java | 17 |
| **Database** | PostgreSQL | 15+ |
| **Cache** | Redis | 7+ |
| **Message Broker** | RabbitMQ | 3+ |
| **Build Tool** | Maven | 3.8+ |
| **Testing** | JUnit 5 + Mockito | Latest |

## **Установка и запуск**

### **1. Клонирование репозитория**

### **2. Запуск через Docker Compose**
```bash
# Запуск всех сервисов
docker-compose up -d

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f app
```

## **API Документация**

### **Subscription API**

#### Активация подписки
```http
POST /api/v1/subscriptions/activate
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "subscriptionType": "PRO",
  "activationDate": "2025-05-05"
}
```

#### Деактивация подписки
```http
POST /api/v1/subscriptions/deactivate
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "subscriptionType": "PRO"
}
```

#### Получение активной подписки
```http
GET /api/v1/subscriptions/{userId}/active
```

#### Получение счетов пользователя
```http
GET /api/v1/subscriptions/{userId}/invoices?page=0&size=20
```

### **Cache API**

#### Получение информации о пользователе
```http
GET /api/v1/cache/users/{userId}/info?page=0&size=20
```

## **Конфигурация**

### **Переменные окружения**
```bash
# Сервер
SERVER_PORT=8090

# База данных
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/subscriptions
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# URL основного сервиса (для fallback)
SERVICE_URL=http://localhost:8090
```

## **Схема базы данных**

### **Таблицы**
- **users** - пользователи
- **subscriptions** - подписки
- **invoices** - счета
- **outbox** - события для отправки в RabbitMQ

### **Индексы**
- Уникальный индекс на активные подписки пользователя
- Индексы для быстрой выборки счетов
- Индексы для outbox обработки

## **Обработка ошибок**

### **Fallback механизмы**
- **Redis недоступен** → запрос в основной сервис
- **RabbitMQ недоступен** → сохранение в Outbox
- **БД недоступна** → возврат ошибки клиенту

### **Коды ошибок**
- `400` - неверные входные данные
- `404` - сущность не найдена  
- `500` - внутренняя ошибка сервера
- `503` - временно недоступен

## **Разработка**

### **Структура проекта**
```
src/main/java/com/xtended/subscriptionservice/
├── cache/                    # Кэш модуль
│   ├── controller/          # Контроллеры кэша
│   ├── service/             # Сервисы кэша
│   └── model/               # Модели кэша
├── subscription/            # Основной модуль
│   ├── controller/          # REST контроллеры
│   ├── service/             # Бизнес-логика
│   ├── repository/          # Доступ к данным
│   ├── model/               # JPA сущности
│   ├── dto/                 # DTO объекты
│   ├── messaging/           # Обработка событий
│   └── handler/             # Обработка ошибок
└── SubscriptionServiceApplication.java
```