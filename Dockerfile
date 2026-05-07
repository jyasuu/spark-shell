# ─── Stage 1: Build fat jar ───────────────────────────────────────────────────
FROM gradle:8.8-jdk21 AS build

WORKDIR /workspace/app

COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN gradle bootJar -x test --no-daemon

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
