
#!/bin/bash

if (( $# != 2 )); then
  echo "Please specify the container name and the version"
  exit 1
fi

NAME=$1
VERSION=$2

cd $NAME/$VERSION && \
docker buildx build --pull . -t openmole/$NAME:$VERSION && \
docker push openmole/$NAME:$VERSION &&
docker rmi openmole/$NAME:$VERSION

