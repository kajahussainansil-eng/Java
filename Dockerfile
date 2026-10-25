# ==============================================================================
# CampusFlow Web Application — Production Dockerfile
# Multi-stage build for ultra-lightweight, zero-dependency deployment
# ==============================================================================

# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY src ./src
COPY static ./static

# Compile Java sources
RUN mkdir -p out && \
    find src -name "*.java" > sources.txt && \
    javac -encoding UTF-8 -d out @sources.txt

# Package standalone executable JAR
RUN mkdir -p /staging && \
    cp -r out/* /staging/ && \
    mkdir -p /staging/static && \
    cp -r static/* /staging/static/ && \
    echo "Manifest-Version: 1.0" > manifest.mf && \
    echo "Main-Class: web.CampusFlowWebServer" >> manifest.mf && \
    echo "Specification-Title: CampusFlow" >> manifest.mf && \
    jar cfm CampusFlowWeb.jar manifest.mf -C /staging .

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for cloud security
RUN addgroup -S campusflow && adduser -S campusflow -G campusflow && \
    mkdir -p /app/web_data && chown -R campusflow:campusflow /app

COPY --from=builder /build/CampusFlowWeb.jar ./CampusFlowWeb.jar
COPY --from=builder /build/static ./static

USER campusflow

ENV PORT=8080
ENV DATA_DIR=/app/web_data

EXPOSE 8080

CMD ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "CampusFlowWeb.jar"]
