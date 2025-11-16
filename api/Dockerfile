FROM bitnami/java:21

WORKDIR /app

COPY --from=hengyunabc/arthas:4.0.5-no-jdk /opt/arthas /opt/arthas

COPY build/libs/gmail-cloud-*.jar /app/app.jar
ENTRYPOINT java -Xss256k -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar --spring.profiles.active=docker
