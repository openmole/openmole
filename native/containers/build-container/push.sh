#!/bin/bash

docker pull debian:testing && \
docker build . -t openmole/build && \ 
docker push openmole/build

