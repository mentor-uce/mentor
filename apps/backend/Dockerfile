# Mentor API - Spring Boot (Java 21, Maven)
# Multi-stage build: compile with Maven, run with a small JRE image.

# ---------- Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Run ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S mentor && adduser -S mentor -G mentor
COPY --from=build /app/target/*.jar app.jar
USER mentor
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
