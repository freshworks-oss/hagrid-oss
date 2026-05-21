FROM maven:3.9.6-eclipse-temurin-21-alpine

WORKDIR /app

# Copy the build configuration configuration layout
COPY hagrid/pom.xml .
COPY hagrid/src ./src

# Configure JVM flags optimized for container environments
ENTRYPOINT ["sh", "-c", "mvn clean compile exec:java  -Dexec.mainClass=com.freshworks.Main -U"]