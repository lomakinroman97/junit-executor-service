# 🚀 Быстрый старт GitHub MCP Service

Этот сервис автоматически собирает данные из вашего GitHub репозитория и отправляет их на email **каждые 3 минуты**.

## 📋 Что делает сервис

- 🔍 Получает информацию о репозитории
- 📝 Собирает последние коммиты
- 🐛 Проверяет открытые issues
- 🔀 Анализирует открытые pull requests
- 📧 Отправляет отчет на email каждые 3 минуты

## 1. Настройка GitHub Token

Создайте Personal Access Token на GitHub:
1. Перейдите в [Settings → Developer settings → Personal access tokens](https://github.com/settings/tokens)
2. Создайте новый токен с правами `repo` и `read:org`
3. Скопируйте токен

## 2. Настройка конфигурации

Отредактируйте `src/main/resources/application.conf`:
```hocon
github {
  mcp {
    token = "your_github_token_here"
  }
  
  repository {
    owner = "your_github_username"
    name = "your_repository_name"
  }
}
```

## 3. Запуск сервиса

### Быстрый запуск:
```bash
./start-service.sh
```

### Ручной запуск:
```bash
./gradlew build
java -jar build/libs/service-kotlin-mcp-github-rest-api-1.0.0.jar
```

## 4. Проверка работы

Сервис начнет работать сразу после запуска:
- 📊 Первый отчет будет отправлен немедленно
- 🔄 Последующие отчеты - каждые 3 минуты
- 📧 Проверьте ваш email: `lomakinrs1997@gmail.com`

## 🛑 Остановка сервиса

Нажмите `Ctrl+C` или найдите PID и остановите:
```bash
ps aux | grep "service-kotlin-mcp-github-rest-api"
kill <PID>
```

## 📝 Логи

Логи записываются в `logs/service.log`:
```bash
tail -f logs/service.log
```

---

**🎯 Цель**: Получать актуальную информацию о GitHub репозитории каждые 3 минуты!

