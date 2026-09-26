# ==============================================================================
# Multi-Stage Dockerfile for Core Banking Ledger Service
# Java 21 / Eclipse Temurin Alpine Distroless Base / Layered Jar Extraction
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package (or Builder Layer Extractor)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy maven wrapper and project definition first for efficient caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Resolve dependencies offline
RUN ./mvnw dependency:go-offline -B

# Copy source code and build production jar (skip tests as CI/CD verifies them)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Extract Spring Boot layered jar for optimal Docker layer caching
WORKDIR /workspace/target
RUN java -Djarmode=layertools -jar ledger-service-0.0.1-SNAPSHOT.jar extract

# ------------------------------------------------------------------------------
# Stage 2: Minimal Distroless Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Create a non-privileged system user for banking security compliance
RUN addgroup -S banking && adduser -S ledger -G banking

# Copy layered jar directories in order of change frequency (dependencies least, application most)
COPY --from=builder /workspace/target/dependencies/ ./
COPY --from=builder /workspace/target/spring-boot-loader/ ./
COPY --from=builder /workspace/target/snapshot-dependencies/ ./
COPY --from=builder /workspace/target/application/ ./

# Change ownership to non-root user
RUN chown -R ledger:banking /app
USER ledger:banking

# Environment configuration defaults
ENV SERVER_PORT=8080 \
    JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
