# syntax=docker/dockerfile:1

FROM gradle:8.9-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]