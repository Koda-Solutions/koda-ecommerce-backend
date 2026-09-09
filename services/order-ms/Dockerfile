# Multi-stage, non-root, healthcheck. Build stage resolves dependencies before
# src is copied (Koda docker.md pattern) so code edits rebuild in seconds and
# never re-download Maven Central in a normal CI run.

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./pom.xml
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre
RUN useradd --create-home --shell /usr/sbin/nologin appuser \
  && apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*
USER appuser
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8070
# MaxRAMPercentage reads the container limit instead of guessing a heap, which
# only works together with a cgroup memory limit (Koda docker.md).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseContainerSupport", "-jar", "app.jar"]
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
  CMD ["curl", "-fsS", "http://localhost:8070/api/order/actuator/health"]
