#!/bin/sh

MEM="512m"

if [ $# -gt 0 ]
then
	MEM=$1	
fi


rm -Rf configuration/org*
rm configuration/*.log
java -Xmx${MEM} -jar plugins/org.eclipse.equinox.launcher_1.1.0.jar\
     -p openmole-plugins,openmole-plugins-ui $@
cat configuration/*.log
