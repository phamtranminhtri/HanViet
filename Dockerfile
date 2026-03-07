# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app
COPY ["gradlew", "gradlew.bat", "./"]
COPY gradle gradle
COPY ["build.gradle", "settings.gradle", "./"]
COPY src src

RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# Run stage
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /app/build/libs/HanViet-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]