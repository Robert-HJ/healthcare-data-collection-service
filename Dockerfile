FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle

RUN chmod +x gradlew
RUN ./gradlew dependencies --configuration runtimeClasspath --no-daemon

COPY src src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system healthcare \
    && useradd --system --gid healthcare healthcare

COPY --from=builder --chown=healthcare:healthcare /workspace/build/libs/application.jar application.jar

USER healthcare

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
