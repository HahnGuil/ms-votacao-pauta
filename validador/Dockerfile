FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY --from=build /app/target/validador-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 26000

ENTRYPOINT ["java", "-jar", "app.jar"]