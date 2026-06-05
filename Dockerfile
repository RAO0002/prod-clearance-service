# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-25 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=builder /build/target/prod-clearance-service-1.0.0.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/api/health/live || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]