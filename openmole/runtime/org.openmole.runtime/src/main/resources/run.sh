#!/bin/sh

MEMORY=$1
shift
java -Xmx${MEMORY} -jar plugins/org.eclipse.equinox.launcher_1.0.200.jar $@ 
