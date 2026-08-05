# =============================================================================
# Stage 1: Build
# Why: Separate build environment keeps the final image lean by excluding the
#      JDK, Maven cache, and source code from the runtime layer.
# =============================================================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy wrapper and POM first to exploit Docker layer caching.
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Pre-fetch all dependencies in a dedicated cached layer.
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B --no-transfer-progress

# Build arguments
ARG SKIP_TESTS=true
ARG SKIP_SONAR=true
ARG SONAR_HOST_URL=http://host.docker.internal:9000
ARG SONAR_TOKEN=""

# Copy application source.
COPY src ./src

# Build JAR (skip tests by default for faster builds; set SKIP_TESTS=false to run full verify)
RUN if [ "$SKIP_TESTS" = "true" ]; then \
      ./mvnw clean package -B --no-transfer-progress -DskipTests; \
    else \
      ./mvnw clean verify -B --no-transfer-progress; \
    fi

# Optional SonarQube analysis
RUN if [ "$SKIP_SONAR" = "false" ] && [ -n "$SONAR_TOKEN" ]; then \
      ./mvnw sonar:sonar \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.token=$SONAR_TOKEN \
        -B --no-transfer-progress; \
    else \
      echo "Skipping SonarQube analysis (SKIP_SONAR=$SKIP_SONAR, SONAR_TOKEN set: $([ -n "$SONAR_TOKEN" ] && echo yes || echo no))"; \
    fi

# =============================================================================
# Stage 2: Runtime
# Why: eclipse-temurin JRE Alpine is ~100 MB vs ~350 MB for the full JDK image.
# =============================================================================
FROM eclipse-temurin:17-jre AS runtime

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

COPY --from=builder /app/target/user-product-service-*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 9091

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider \
      http://localhost:9091/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
