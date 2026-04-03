#!/bin/bash
set -e

mongoimport --host localhost \
            --db seedsdb \
            --collection seed \
            --type json \
            --file /data/seeds.json \
            --jsonArray \
            --drop

