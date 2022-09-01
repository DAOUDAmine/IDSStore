mvn package
docker build . -t mondaine-hub
docker tag mondaine-hub:latest ci.tno.nl/esdl/mondaine-hub
docker push ci.tno.nl/esdl/mondaine-hub:latest
