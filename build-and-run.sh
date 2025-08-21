#!/bin/bash

echo "Building JUnit Executor Service..."

# Build the project
./gradlew clean build

if [ $? -eq 0 ]; then
    echo "Build successful!"
    
    echo "Building Docker image..."
    docker build -t junit-executor .
    
    if [ $? -eq 0 ]; then
        echo "Docker image built successfully!"
        
        echo "Starting service with Docker Compose..."
        docker-compose up -d
        
        echo "Service started! Check logs with: docker-compose logs -f"
        echo "Health check: http://localhost:8080/health"
        echo "API endpoint: http://localhost:8080/api/execute"
    else
        echo "Docker build failed!"
        exit 1
    fi
else
    echo "Gradle build failed!"
    exit 1
fi
