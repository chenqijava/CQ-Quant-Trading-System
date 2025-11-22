./gradlew assemble
docker rmi -f api-main:latest
docker rmi -f api:0.0.1
docker build . -t
