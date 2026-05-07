# ─── Stage 1: Build native image with GraalVM ────────────────────────────────
FROM ghcr.io/graalvm/native-image-community:21 AS build

WORKDIR /workspace/app

# Copy Gradle wrapper and config first (layer cache)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source
COPY src src

# Make gradlew executable and build native image
RUN chmod +x gradlew && \
    ./gradlew nativeCompile -x test --no-daemon

# ─── Stage 2: Minimal runtime image ──────────────────────────────────────────
FROM alpine

# gcompat provides glibc compatibility needed by GraalVM native binaries
RUN apk add --no-cache gcompat

COPY --from=build /workspace/app/build/native/nativeCompile/sparkctl /usr/bin/sparkctl

ENTRYPOINT ["sparkctl"]
