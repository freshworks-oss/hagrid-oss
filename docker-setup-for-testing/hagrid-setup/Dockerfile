FROM maven:3.9.6-eclipse-temurin-21-alpine

WORKDIR /app

RUN apk add --no-cache git


# Copy the build configuration configuration layout
COPY branch_test.sh .


RUN chmod +x branch_test.sh

# Configure JVM flags optimized for container environments
ENTRYPOINT ["./branch_test.sh"]