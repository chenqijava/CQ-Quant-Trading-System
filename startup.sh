git pull

cd api
bash ./build.sh

docker rm -f api-main
docker-compose up -d

