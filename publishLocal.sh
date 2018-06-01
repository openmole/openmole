git submodule init
git submodule update
git lfs fetch

(cd build-system && sbt clean publishLocal)
(cd libraries && sbt clean publishLocal)
(cd openmole && sbt clean publishLocal)

