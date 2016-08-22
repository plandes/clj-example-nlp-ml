#!/bin/sh

docker-machine ssh default -t -f -N -L 9200:localhost:9200
