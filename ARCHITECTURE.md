# Архитектура JUnit Executor Service

## Обзор системы

JUnit Executor Service - это сервис для автоматической генерации и выполнения JUnit 5 тестов на Kotlin коде с использованием LLM (Yandex GPT).

## Архитектурная диаграмма

```
Mobile App → REST API → Security Service → Yandex GPT → Test Generation → Test Execution → Results
                                    ↓
                              Safe Code Execution
```

## Основные компоненты

### 1. REST API (Ktor)
- **Endpoint**: `POST /api/execute`
- **Функция**: Прием Kotlin кода от мобильных приложений
- **Ответ**: Результаты выполнения тестов + результаты выполнения кода

### 2. Security Service
- **Функция**: Валидация безопасности входящего кода
- **Защита**: Черный список опасных конструкций
- **Проверки**: System.exit, Runtime.exec, ProcessBuilder и др.

### 3. Yandex GPT Service
- **Функция**: Генерация JUnit 5 тестов на основе входящего кода
- **Вход**: Kotlin код
- **Выход**: Сгенерированные JUnit тесты

### 4. Test Execution Service
- **Функция**: Компиляция и выполнение JUnit тестов
- **Технологии**: 
  - Kotlin Compiler Embeddable
  - JUnit Platform Launcher
  - URLClassLoader для динамической загрузки классов

### 5. Safe Code Executor
- **Функция**: Безопасное выполнение входящего кода
- **Особенности**: 
  - Таймауты выполнения
  - Изоляция процессов
  - Ограничение ресурсов

## Процесс выполнения тестов

### Шаг 1: Валидация безопасности
```kotlin
val securityValidation = securityService.validateCode(kotlinCode)
if (!securityValidation.isValid) {
    return errorResponse(securityValidation.error)
}
```

### Шаг 2: Генерация тестов
```kotlin
val generatedTestCode = yandexGPTService.generateTests(kotlinCode)
```

### Шаг 3: Безопасное выполнение кода
```kotlin
val codeExecutionResult = safeCodeExecutor.executeCodeSafely(kotlinCode)
```

### Шаг 4: Компиляция кода
```kotlin
// Компиляция основного кода
val mainClassFile = compileKotlinCode(kotlinCode, "UserCode")

// Компиляция тестов
val testClassFile = compileKotlinCode(kotlinTestCode, "GeneratedTests")
```

### Шаг 5: Выполнение JUnit тестов
```kotlin
val launcher = LauncherFactory.create()
val summaryListener = SummaryGeneratingListener()
launcher.registerTestExecutionListeners(summaryListener)

val request = LauncherDiscoveryRequestBuilder
    .request()
    .selectors(DiscoverySelectors.selectClass(testClass))
    .build()

launcher.execute(request)
```

## Технические детали

### Компиляция Kotlin
- **Компилятор**: K2JVMCompiler (новый Kotlin compiler)
- **Цель**: JVM 19
- **Выход**: .class файлы во временной директории

### JUnit Test Execution
- **Launcher**: JUnit Platform Launcher
- **Discovery**: Автоматическое обнаружение тестов
- **Listeners**: SummaryGeneratingListener для сбора результатов
- **Class Loading**: Динамическая загрузка скомпилированных классов

### Безопасность
- **Изоляция**: Временные директории для каждого выполнения
- **Таймауты**: Ограничение времени выполнения
- **Ресурсы**: Ограничение памяти и CPU
- **Cleanup**: Автоматическая очистка временных файлов

## Конфигурация

```hocon
execution {
  timeout-seconds = 60
  max-code-length = 10000
  code-execution-timeout = 30
  max-memory-mb = 512
  enable-sandbox = true
}
```

## Обработка ошибок

### Типы ошибок
1. **SECURITY_ERROR** - Код не прошел проверку безопасности
2. **COMPILATION_ERROR** - Ошибка компиляции Kotlin кода
3. **TEST_COMPILATION_ERROR** - Ошибка компиляции тестов
4. **TEST_EXECUTION_ERROR** - Ошибка выполнения тестов
5. **TIMEOUT_ERROR** - Превышение времени выполнения
6. **RUNTIME_ERROR** - Ошибка во время выполнения

### Логирование
- Все этапы выполнения логируются
- Ошибки компиляции сохраняются в деталях
- Результаты тестов включают детальную информацию

## Масштабирование

### Текущие ограничения
- Синхронное выполнение
- Один поток на запрос
- Временные файлы в памяти

### Планы по улучшению
- Асинхронное выполнение
- Пул потоков для тестов
- Кэширование результатов
- Распределенное выполнение

## Мониторинг

### Метрики
- Время компиляции
- Время выполнения тестов
- Количество успешных/неуспешных тестов
- Использование ресурсов

### Логи
- Структурированное логирование
- Трассировка запросов
- Детали ошибок
