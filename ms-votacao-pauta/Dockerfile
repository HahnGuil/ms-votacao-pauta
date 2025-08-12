FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Give execute permission to Maven wrapper
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy jar from build stage
COPY --from=build /app/target/ms-votacao-pauta-0.0.1-SNAPSHOT.jar app.jar

# Run the application with docker profile
EXPOSE 30000
CMD ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]