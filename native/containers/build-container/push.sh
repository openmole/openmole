#!/bin/bash

docker pull ubuntu:22.04 && \
docker build . -t openmole/build && \ 
docker push openmole/build

