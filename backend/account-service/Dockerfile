# ==============================================================================
# Multi-Stage Dockerfile for Core Banking Account Service
# Java 21 / Eclipse Temurin Alpine Base / Layered Jar Extraction
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Layer Extractor
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /workspace/target
COPY target/*.jar ./app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ------------------------------------------------------------------------------
# Stage 2: Minimal Distroless Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN addgroup -S banking && adduser -S account -G banking

COPY --from=builder /workspace/target/extracted/dependencies/ ./
COPY --from=builder /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/target/extracted/application/ ./

RUN chown -R account:banking /app
USER account:banking

ENV SERVER_PORT=8082 \
    JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8082

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
