# syntax=docker/dockerfile:1

# --- Etapa de build: usa a imagem oficial do Gradle com JDK 21 ---
FROM gradle:8.9-jdk21 AS build
WORKDIR /workspace

COPY settings.gradle.kts gradle.properties* ./
COPY build.gradle.kts build.gradle.kts
COPY src src

RUN gradle bootJar --no-daemon -x test

# --- Etapa final: só o JRE + o jar, imagem enxuta ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
