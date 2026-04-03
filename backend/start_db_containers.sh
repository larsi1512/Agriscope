#!/bin/bash

# Start MongoDB for usersdb on port 27017
docker run --name usersdb -d -p 27017:27017 mongo

# Start MongoDB for farmsdb on port 27018
docker run --name farmsdb -d -p 27018:27017 mongo

# Start MongoDB for seedsdb on port 27019
docker run --name seedsdb \
  -d \
  -p 27019:27017 \
  -v "$(pwd)/seeds.json":/data/seeds.json \
  -v "$(pwd)/init-mongo.sh":/docker-entrypoint-initdb.d/init-mongo.sh \
  mongo

# Output the status of the containers
docker ps