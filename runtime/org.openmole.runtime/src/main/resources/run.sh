#!/bin/sh

MEMORY=$1
shift
java -Xmx${MEMORY} -Dosgi.classloader.singleThreadLoads=true -jar plugins/org.eclipse.equinox.launcher.jar $@ 
