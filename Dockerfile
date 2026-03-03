FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

LABEL org.opencontainers.image.source="https://github.com/UmarHayat15/MentorMian-Backend"
LABEL org.opencontainers.image.description="AI Tutor Backend - RAG-powered tutoring API"
LABEL org.opencontainers.image.licenses="MIT"

COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
