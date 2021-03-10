#!/bin/bash

docker pull ubuntu:20.04
docker build . -t openmole/build 
docker push openmole/build

