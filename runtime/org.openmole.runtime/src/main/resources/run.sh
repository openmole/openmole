#!/bin/sh

MEMORY=$1
shift

CONFIGDIR=$1
shift

cp -r configuration ${CONFIGDIR}

java -Xmx${MEMORY} -Dosgi.configuration.area=${CONFIGDIR} -Dosgi.classloader.singleThreadLoads=true -XX:+UseCompressedOops -XX:+CMSClassUnloadingEnabled -XX:+UseParallelGC -jar plugins/org.eclipse.equinox.launcher.jar $@ 

rm -rf ${CONFIGDIR}

