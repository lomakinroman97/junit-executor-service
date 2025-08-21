FROM openjdk:19-jdk-slim

# Set working directory
WORKDIR /app

# Copy gradle files
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew build --no-daemon

# Create runtime directory
RUN mkdir -p /app/runtime

# Set security manager properties
ENV JAVA_OPTS="-Djava.security.manager=allow -Djava.security.policy=/app/security.policy"

# Create security policy file
RUN echo "grant {" > /app/security.policy && \
    echo "  permission java.lang.RuntimePermission \"setSecurityManager\";" >> /app/security.policy && \
    echo "  permission java.lang.RuntimePermission \"createSecurityManager\";" >> /app/security.policy && \
    echo "  permission java.lang.RuntimePermission \"setIO\";" >> /app/security.policy && \
    echo "  permission java.io.FilePermission \"/tmp/*\", \"read,write,delete\";" >> /app/security.policy && \
    echo "  permission java.io.FilePermission \"/app/runtime/*\", \"read,write,delete\";" >> /app/security.policy && \
    echo "  permission java.lang.reflect.ReflectPermission \"suppressAccessChecks\";" >> /app/security.policy && \
    echo "  permission java.util.PropertyPermission \"*\", \"read\";" >> /app/security.policy && \
    echo "};" >> /app/security.policy

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "build/libs/junit-executor-server-1.0.0.jar"]
