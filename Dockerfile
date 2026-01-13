# Build stage
FROM gradle:8.14.3-jdk17 AS build
WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

# Copy source code
COPY src ./src

# Build
RUN ./gradlew clean build -x check -x test -Pproduction

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port (Railway가 자동으로 PORT 환경변수 설정)
EXPOSE 8080

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
