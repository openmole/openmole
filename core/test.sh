
CUR=$PWD

cd openmole/misc/org.openmole.misc.logging/
mvn clean install
cd $CUR

cd ui
mvn clean install

cd openmole-ui-console/openmole-ui-console-package/target/openmole/
./openmole-console

cd $CUR

