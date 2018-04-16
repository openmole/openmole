git submodule init
git submodule update
git lfs fetch

(cd build-system && sbt publishLocal)
(cd libraries && sbt publishLocal)
(cd openmole && sbt "project openmole" assemble)

(echo openmole is ready in openmole/bin/openmole/target/assemble/)

