#!/bin/bash

# Stop the usersdb container
docker stop usersdb

# Remove the usersdb container
docker rm usersdb

# Stop the farmsdb container
docker stop farmsdb

# Remove the farmsdb container
docker rm farmsdb

# Stop the seedsdb container
docker stop seedsdb

# Remove the seedsdb container
docker rm seedsdb

# Output the status of the containers
docker ps -a
