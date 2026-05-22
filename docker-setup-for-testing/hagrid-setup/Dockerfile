FROM maven:3.9.6-eclipse-temurin-21-alpine

WORKDIR /app

RUN apk add --no-cache git


# Copy the build configuration configuration layout
COPY performance_test.sh .


RUN chmod +x performance_test.sh

# Configure JVM flags optimized for container environments
ENTRYPOINT ["./performance_test.sh"]