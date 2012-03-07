#!/bin/sh

MEM="512m"

if [ $# -gt 0 ]
then
	MEM=$1	
fi

rm -Rf configuration/org*
rm configuration/*.log
java -ea -Xmx${MEM} -Dosgi.classloader.singleThreadLoads=true -XX:+UseCompressedOops -XX:MaxPermSize=128M -XX:+UseParallelGC \
     -jar plugins/org.eclipse.equinox.launcher.jar \
     -p openmole-plugins,openmole-plugins-ui $@
cat configuration/*.log
