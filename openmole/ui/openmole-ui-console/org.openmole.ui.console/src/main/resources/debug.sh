#!/bin/sh

MEM="512m"

if [ $# -gt 0 ]
then
	MEM=$1	
fi

rm -Rf configuration/org*
rm configuration/*.log
java -Xdebug -Xnoagent \
  -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n \
  -Xmx${MEM} -jar plugins/org.eclipse.equinox.launcher_1.0.200.jar\
  -p openmole-plugins,openmole-plugins-ui $@
cat configuration/*.log
