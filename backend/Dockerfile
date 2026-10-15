FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S -g 10001 campushire \
    && adduser -S -D -H -u 10001 -G campushire campushire
WORKDIR /app
COPY --from=build --chown=campushire:campushire /workspace/target/campushire-api-0.0.1-SNAPSHOT.jar app.jar

USER campushire
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=5s --start-period=45s --retries=5 \
    CMD-SHELL curl --fail --silent "http://localhost:${PORT:-8080}/actuator/health/readiness" || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
