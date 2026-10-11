# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health check
RUN apk add --no-cache curl && \
    addgroup -S apihealth && \
    adduser -S apihealth -G apihealth

# Copy jar from builder stage
COPY --from=builder /build/target/api-health-platform-*.jar app.jar
RUN chown -R apihealth:apihealth /app

USER apihealth

# Production JVM tuning
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
