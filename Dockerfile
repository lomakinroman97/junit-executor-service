# Используем локальный образ с Java
FROM github-mcp-service:latest

# Устанавливаем рабочую директорию
WORKDIR /app

# Попробуем установить Kotlin compiler с правами root
USER root
RUN apt-get update && apt-get install -y curl unzip && \
    curl -L -o kotlin-compiler.zip https://github.com/JetBrains/kotlin/releases/download/v1.9.24/kotlin-compiler-1.9.24.zip && \
    unzip kotlin-compiler.zip && \
    mv kotlinc /usr/local/ && \
    chmod +x /usr/local/kotlinc/bin/kotlinc && \
    rm kotlin-compiler.zip && \
    apt-get remove -y curl unzip && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Копируем собранный JAR файл
COPY build/libs/junit-executor-server-1.0.0.jar app.jar

# Создаем директорию для зависимостей
RUN mkdir -p /app/deps

# Копируем JUnit и Hamcrest JAR файлы
COPY build/dependencies/ /app/deps/

# Открываем порт
EXPOSE 8080

# Запускаем приложение
CMD ["java", "-jar", "app.jar"]
