# syntax=docker/dockerfile:1.7

# Build stage
FROM gradle:8.14.3-jdk17 AS build
WORKDIR /app

# Dependency layer cache
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# Source
COPY src ./src

# Build
RUN gradle clean bootJar --no-daemon -x test -x check -Pproduction

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
