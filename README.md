# JUnit Executor Service

Сервис для автоматической генерации и выполнения JUnit 4 тестов на Kotlin с использованием Yandex GPT.

## 🚀 Возможности

- **REST API** для приема Kotlin кода от мобильных приложений
- **Интеграция с Yandex GPT** для автоматической генерации JUnit 4 тестов
- **Выполнение тестов** в изолированной среде с помощью Kotlin compiler
- **Компиляция Kotlin кода** с использованием `kotlinc`
- **Выполнение JUnit 4 тестов** с помощью JUnit Core
- **Валидация безопасности** входящего кода
- **Docker поддержка** с предустановленным Kotlin compiler
- **Изоляция выполнения** с таймаутами и ограничениями

## 🏗️ Архитектура

```
Mobile App → REST API → Security Validation → Yandex GPT → Test Generation → Code Compilation → Test Execution → Results
```

## 📋 API Endpoints

### POST /api/execute
Принимает Kotlin код, генерирует тесты и выполняет их.

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
  "testResults": [
    {
      "testName": "testAdd",
      "status": "PASSED",
      "executionTime": 15
    },
    {
      "testName": "testAddNegativeNumbers", 
      "status": "PASSED",
      "executionTime": 12
    }
  ],
  "originalCode": "fun add(a: Int, b: Int): Int { return a + b }",
  "generatedTestCode": "import org.junit.Test...",
  "compilationInfo": {
    "success": true,
    "classFiles": ["CombinedTestsKt.class", "GeneratedTests.class"]
  },
  "executionSummary": {
    "totalTests": 4,
    "passedTests": 4,
    "failedTests": 0,
    "totalExecutionTime": 45
  }
}
```

### GET /health
Проверка состояния сервиса.

## 🛠️ Технологии

- **Kotlin** - основной язык разработки
- **Ktor** - HTTP сервер
- **Yandex GPT** - генерация JUnit 4 тестов
- **JUnit 4 (JUnit Core)** - фреймворк тестирования
- **Kotlin Compiler (kotlinc)** - компиляция Kotlin кода
- **Hamcrest** - библиотека матчеров для JUnit
- **Docker** - контейнеризация
- **Gradle** - сборка проекта

## 📦 Установка и запуск

### Предварительные требования

- Java 19+
- Docker (для контейнеризованного запуска)
- API ключ Yandex GPT

### Локальный запуск

1. Клонируйте репозиторий
2. Создайте файл `.env` с вашими API ключами:
   ```bash
   YANDEX_GPT_API_KEY=your_api_key_here
   YANDEX_GPT_FOLDER_ID=your_folder_id_here
   ```
3. Запустите сервис:
   ```bash
   source .env && ./gradlew run
   ```

### Docker запуск

1. Соберите проект:
   ```bash
   ./gradlew clean build
   ```

2. Создайте `.env` файл с API ключами:
   ```bash
   YANDEX_GPT_API_KEY=your_api_key_here
   YANDEX_GPT_FOLDER_ID=your_folder_id_here
   ```

3. Запустите через Docker Compose:
   ```bash
   docker-compose up --build
   ```

Или соберите и запустите Docker образ вручную:
```bash
docker build -t junit-executor-service .
docker run -p 8080:8080 \
  -e YANDEX_GPT_API_KEY=your_api_key \
  -e YANDEX_GPT_FOLDER_ID=your_folder_id \
  junit-executor-service
```

## 🔧 Конфигурация

### Переменные окружения

- `YANDEX_GPT_API_KEY` - API ключ Yandex GPT
- `YANDEX_GPT_FOLDER_ID` - ID папки Yandex Cloud
- `JAVA_OPTS` - опции Java (по умолчанию: `-Xmx512m`)

### Основные настройки в `src/main/resources/application.conf`

- Порт сервера (по умолчанию: 8080)
- Таймауты выполнения
- Настройки безопасности

## 📊 Статус разработки

### ✅ Полностью реализовано

- **REST API** с Ktor для приема Kotlin кода
- **Интеграция с Yandex GPT** для генерации JUnit 4 тестов
- **Выполнение сгенерированных тестов** в изолированной среде
- **Компиляция Kotlin кода** с помощью внешнего `kotlinc` compiler
- **Выполнение JUnit 4 тестов** с помощью JUnit Core
- **Безопасное выполнение кода** с таймаутами и ограничениями
- **Валидация безопасности** входящего кода
- **Docker контейнеризация** с предустановленным Kotlin compiler
- **Автоматическое копирование зависимостей** (JUnit, Hamcrest) в Docker образ
- **Обработка ошибок компиляции и выполнения**
- **Очистка временных файлов** после выполнения

### 🔄 Возможные улучшения

- Кэширование результатов компиляции
- Метрики производительности
- Расширенная диагностика ошибок
- Поддержка JUnit 5 (Jupiter)
- Параллельное выполнение тестов

### 📋 Планируется
- Кэширование результатов
- Метрики и мониторинг
- Масштабирование

## 🧪 Тестирование

### Тест API

```bash
# Простая функция
curl -X POST http://localhost:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"code": "fun add(a: Int, b: Int): Int { return a + b }"}'

# Функция с несколькими параметрами
curl -X POST http://localhost:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"code": "fun multiply(x: Int, y: Int): Int { return x * y }"}'

# Проверка здоровья сервиса
curl http://localhost:8080/health
```

### Ожидаемый результат

Сервис должен:
1. ✅ Принять Kotlin код
2. ✅ Сгенерировать JUnit 4 тесты через Yandex GPT
3. ✅ Скомпилировать код и тесты с помощью `kotlinc`
4. ✅ Выполнить сгенерированные тесты
5. ✅ Вернуть реальные результаты выполнения

## 🐳 Docker особенности

- **Kotlin Compiler** предустановлен в контейнере
- **JUnit 4 и Hamcrest** JAR файлы автоматически копируются
- **Изоляция выполнения** для безопасности
- **Оптимизированный размер** образа

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
