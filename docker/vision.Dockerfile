# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY backend/ .
RUN sh ./gradlew --no-daemon test installDist

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* && useradd --system --uid 10001 loopy
WORKDIR /app
COPY --from=build --chown=loopy:loopy /src/build/install/loopy-backend/ /app/
USER loopy
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=4s --start-period=15s --retries=10 CMD curl -fsS http://localhost:8080/ready || exit 1
ENTRYPOINT ["/app/bin/loopy-backend"]
