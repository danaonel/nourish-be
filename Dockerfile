# Multi-stage build for the Nourish backend (Spring Boot, Java 17).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/nourish-backend-1.0.0.jar app.jar
EXPOSE 8080
# Configure at runtime via env vars, e.g.:
#   PORT, FRONTEND_ORIGINS, ANTHROPIC_API_KEY, ANTHROPIC_MODEL
ENTRYPOINT ["java", "-jar", "app.jar"]
