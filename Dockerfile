# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Dependencies first, so they're cached until pom.xml changes.
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B -DskipTests package

# ---- Run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 fittracker \
    && mkdir -p /app/uploads \
    && chown fittracker /app/uploads
COPY --from=build /app/target/*.jar app.jar
USER fittracker
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
# Config comes from environment variables: DB_URL, DB_USERNAME, DB_PASSWORD, MAIL_USERNAME, MAIL_PASSWORD,
# APP_FRONTEND_URL, APP_CORS_ORIGINS, SESSION_COOKIE_SECURE (see application.properties).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
