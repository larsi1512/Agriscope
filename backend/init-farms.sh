#!/bin/bash
set -e

mongoimport --host localhost \
            --db farmsdb \
            --collection seed \
            --type json \
            --file /data/seeds.json \
            --jsonArray \
            --drop