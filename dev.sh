#!/bin/bash

(cd native/containers/dev-container && ./build.sh)

docker volume create openmole-dev-cache
docker run -ti -h openmole-dev -w /home/$USER/openmole \
  -v openmole-dev-cache:/home/$USER \
  -v $PWD:/home/$USER/openmole \
  -v $HOME/.ssh:/home/$USER/.ssh \
  -u $USER openmole-dev bash 

