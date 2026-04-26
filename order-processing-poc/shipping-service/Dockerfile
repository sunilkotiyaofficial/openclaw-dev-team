# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent + module POMs first for dependency caching
COPY pom.xml .
COPY common-events/pom.xml common-events/
COPY common-kafka/pom.xml common-kafka/
COPY kafka-config/pom.xml kafka-config/
COPY shipping-service/pom.xml shipping-service/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY inventory-service/pom.xml inventory-service/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl shipping-service -am -q

# Copy source
COPY common-events/src common-events/src
COPY common-kafka/src common-kafka/src
COPY kafka-config/src kafka-config/src
COPY shipping-service/src shipping-service/src

# Build
RUN mvn package -pl shipping-service -am -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy AS runtime
RUN groupadd --gid 1001 spring && useradd --uid 1001 --gid spring --no-create-home spring
WORKDIR /app
COPY --from=build /app/shipping-service/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=90s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
