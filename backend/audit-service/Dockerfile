# ==============================================================================
# Multi-Stage Dockerfile for Core Banking Audit Service
# Java 21 / Eclipse Temurin Alpine Base / Layered Jar Extraction
# Reference: docs/ENTERPRISE_ARCHITECTURE_DEFENSE_DOSSIER.md
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

# Create a non-privileged system user for banking security compliance
RUN addgroup -S banking && adduser -S audit -G banking

# Copy layered jar directories in order of change frequency
COPY --from=builder /workspace/target/extracted/dependencies/ ./
COPY --from=builder /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/target/extracted/application/ ./

# Change ownership to non-root user
RUN chown -R audit:banking /app
USER audit:banking

# Environment configuration defaults
ENV SERVER_PORT=8085 \
    JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8085

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
