#!/bin/bash

docker build --build-arg UID=$(id -u) --build-arg GID=$(id -g) --build-arg GID=$(id -g) --build-arg USER_NAME=$(whoami) -t openmole-dev .
