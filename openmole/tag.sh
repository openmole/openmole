#!/bin/bash

echo "ThisBuild / version := \"$1\"" >version.sbt
git add version.sbt
git commit -m "[Build] set version $1"
git push
git tag -d v$1
git tag v$1
git push --tags --force


