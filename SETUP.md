# Настройка JUnit Executor Service

## Получение API ключей Yandex GPT

Для работы сервиса необходимо получить API ключи от Yandex Cloud:

1. Зарегистрируйтесь в [Yandex Cloud](https://cloud.yandex.ru/)
2. Создайте платежный аккаунт
3. Создайте каталог (folder)
4. Включите сервис Yandex GPT
5. Создайте API ключ

## Настройка конфигурации

1. Скопируйте `src/main/resources/application.conf` в `src/main/resources/application-local.conf`
2. В `application-local.conf` замените placeholder'ы на реальные значения:

```hocon
yandex-gpt {
  api-key = "ваш_реальный_api_ключ"
  folder = "ваш_реальный_folder_id"
  url = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
  timeout-seconds = 30
}
```

## Запуск сервиса

```bash
./gradlew run
```

## Тестирование API

```bash
curl -X POST http://localhost:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"code": "fun add(a: Int, b: Int): Int { return a + b }"}'
```

## Docker запуск

```bash
./gradlew clean build
docker build -t junit-executor-service .
docker run -p 8080:8080 junit-executor-service
```
