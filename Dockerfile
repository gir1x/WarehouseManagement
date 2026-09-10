# Multi-stage build: compile with a full Maven+JDK image, then run on a
# minimal JRE-only image so the final container is much smaller and has a
# reduced attack surface (no build tools shipped in production).

# --- Stage 1: build the jar --------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first and download dependencies separately — Docker caches
# this layer, so re-builds after only changing Java source skip re-downloading
# every dependency from Maven Central.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Stage 2: run the jar -----------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/wms-0.0.1-SNAPSHOT.jar app.jar

# Render (and most PaaS free tiers) set PORT themselves and route traffic to
# it — application.yml already reads ${PORT:8080}, so nothing else to wire.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
