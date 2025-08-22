# JUnit Executor Service

Сервис для автоматической генерации и выполнения JUnit 5 тестов на Kotlin с использованием Yandex GPT.

## 🚀 Возможности

- **REST API** для приема Kotlin кода от мобильных приложений
- **Интеграция с Yandex GPT** для автоматической генерации тестов
- **Валидация кода** с проверкой структуры и синтаксиса
- **Безопасность** с черным списком опасных конструкций
- **Docker поддержка** для легкого развертывания

## 🏗️ Архитектура

```
Mobile App → REST API → Security Service → Yandex GPT → Test Validation → Response
```

## 📋 API Endpoints

### POST /api/execute
Принимает Kotlin код и возвращает сгенерированные тесты.

**Request:**
```json
{
  "code": "fun add(a: Int, b: Int): Int { return a + b }"
}
```

**Response:**
```json
{
  "success": true,
  "testResults": [...],
  "originalCode": "fun add(a: Int, b: Int): Int { return a + b }",
  "generatedTestCode": "import org.junit.jupiter.api.Assertions...",
  "codeExecutionResult": {
    "success": true,
    "output": "Code executed successfully",
    "executionTime": 15
  }
}
```

### GET /health
Проверка состояния сервиса.

## 🛠️ Технологии

- **Kotlin** - основной язык
- **Ktor** - HTTP сервер
- **Yandex GPT** - генерация тестов
- **JUnit 5** - фреймворк тестирования
- **Docker** - контейнеризация
- **Gradle** - сборка

## 📦 Установка и запуск

### Локальный запуск

1. Клонируйте репозиторий
2. Настройте API ключи (см. [SETUP.md](SETUP.md))
3. Запустите сервис:

```bash
./gradlew run
```

### Docker запуск

```bash
./gradlew clean build
docker build -t junit-executor-service .
docker run -p 8080:8080 junit-executor-service
```

## 🔧 Конфигурация

Основные настройки в `src/main/resources/application.conf`:

- Порт сервера
- API ключи Yandex GPT
- Таймауты выполнения
- Безопасность

## 📊 Статус разработки

### ✅ Реализовано
- REST API с Ktor
- Интеграция с Yandex GPT
- Генерация JUnit 5 тестов на Kotlin
- **Реальное выполнение сгенерированных тестов**
- **Компиляция Kotlin кода с помощью Kotlin compiler**
- **JUnit 5 test runner для выполнения тестов**
- **Безопасное выполнение кода с таймаутами**
- Валидация структуры кода
- Безопасность (черный список)
- Docker контейнеризация

### 🔄 В разработке
- Улучшение безопасности выполнения кода
- Оптимизация производительности компиляции
- Расширенная диагностика ошибок

### 📋 Планируется
- Кэширование результатов
- Метрики и мониторинг
- Масштабирование

## 🧪 Тестирование

```bash
# Тест API
curl -X POST http://localhost:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"code": "fun add(a: Int, b: Int): Int { return a + b }"}'

# Проверка здоровья
curl http://localhost:8080/health
```

## 📝 Лицензия

MIT License

## 🤝 Вклад в проект

1. Fork репозитория
2. Создайте feature branch
3. Commit изменения
4. Push в branch
5. Создайте Pull Request

## 📞 Поддержка

По вопросам и предложениям создавайте Issues в репозитории.
