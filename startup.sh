git pull

cd api
bash ./build.sh

cd ..
docker-compose down
docker-compose up -d

