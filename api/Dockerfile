FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY --from=hengyunabc/arthas:4.0.5-no-jdk /opt/arthas /opt/arthas

COPY build/libs/api-*.jar /app/app.jar
ENTRYPOINT java -Xss256k -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar --spring.profiles.active=docker
