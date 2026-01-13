# Build stage
FROM gradle:8.14.3-jdk17 AS build
WORKDIR /app

# Copy all files
COPY . .

# build
RUN gradle clean build -x check -x test -Pproduction

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]