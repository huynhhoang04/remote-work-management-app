# syntax=docker/dockerfile:1
# Backend Spring Boot (modular monolith) — build đa tầng: Maven build -> JRE 21 runtime
# Dùng image maven có sẵn Maven (không phụ thuộc mvnw tải Maven lúc build).
# @reference docs/plan-deploy/01-ci-cd-phase-1.md §4.1

FROM docker.io/library/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM docker.io/library/eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /workspace/target/*.jar /app/app.jar
# Fallback STORAGE_TYPE=local cần thư mục ghi được; production dùng Supabase thì không dùng tới.
RUN mkdir -p /app/uploads && chown -R app:app /app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Duser.timezone=UTC"
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
