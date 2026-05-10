# ── Runtime only ──────────────────────────────────────────────────────────────
# The JAR is built on the host with: mvnw clean package -DskipTests
# Docker only copies the result — no Maven, no downloads, no SSL issues.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
