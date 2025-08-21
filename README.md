# JUnit Executor Service

Сервис для выполнения Kotlin кода с автоматической генерацией и запуском JUnit тестов через LLM (Yandex GPT).

## Функциональность

- **REST API**: Принимает Kotlin код от мобильного приложения
- **LLM Integration**: Генерирует JUnit тесты через Yandex GPT
- **Test Execution**: Валидирует код и тесты (в текущей версии - mock режим)
- **Security**: Проверяет код на опасные конструкции
- **Results**: Возвращает детальные результаты выполнения тестов

## API Endpoints

### POST /api/execute

**Request Body:**
```json
{
  "code": "class MathUtils {\n    fun sum(a: Int, b: Int): Int {\n        return a + b\n    }\n}"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "testResults": [
    {
      "testName": "Code Validation",
      "status": "PASSED",
      "assertions": ["Kotlin code syntax is valid", "Test code syntax is valid"]
    },
    {
      "testName": "LLM Integration",
      "status": "PASSED",
      "assertions": ["Successfully generated tests from LLM"]
    }
  ],
  "originalCode": "...",
  "generatedTestCode": "..."
}
```

**Error Response (4xx/5xx):**
```json
{
  "success": false,
  "error": "COMPILATION_ERROR",
  "details": "Detailed error message"
}
```

### GET /health

Health check endpoint.

## Конфигурация

Основные настройки в `src/main/resources/application.conf`:

- **Server**: Порт и хост
- **Yandex GPT**: API ключ, folder ID, URL
- **Execution**: Таймауты и ограничения
- **Security**: Черный список опасных паттернов

## Запуск

### Локально

```bash
./gradlew run
```

### В Docker (когда Docker доступен)

```bash
# Сборка и запуск
./build-and-run.sh

# Или вручную
./gradlew build
docker build -t junit-executor .
docker-compose up -d
```

### Проверка

```bash
# Health check
curl http://localhost:8080/health

# Тестовый запрос
curl -X POST http://localhost:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"code": "class Test { fun hello() = \"Hello World\" }"}'
```

## Архитектура

- **Main**: Точка входа и HTTP сервер
- **CodeExecutor**: Основной координатор процесса
- **YandexGPTService**: Интеграция с LLM
- **SecurityService**: Проверка безопасности кода
- **TestExecutionService**: Валидация и выполнение тестов (mock режим)

## Текущий статус

✅ **Работает:**
- REST API endpoint
- Интеграция с Yandex GPT
- Генерация Kotlin тестов
- Проверка безопасности кода
- Mock выполнение тестов

🔄 **В разработке:**
- Реальное выполнение Kotlin тестов
- Компиляция Kotlin кода
- JUnit test runner

## Безопасность

- Проверка на черный список опасных паттернов
- Ограничение длины кода
- Таймауты на выполнение
- Изоляция в Docker контейнере

## Требования

- Java 19+ (JDK, не JRE)
- Docker (для контейнеризации)
- Gradle 7.0+

## Планы развития

1. **Реальное выполнение тестов**: Интеграция с Kotlin compiler и JUnit runner
2. **Улучшенная валидация**: Проверка синтаксиса Kotlin
3. **Кэширование**: Кэш для часто используемых тестов
4. **Мониторинг**: Метрики и логирование
5. **Масштабирование**: Поддержка множественных запросов
