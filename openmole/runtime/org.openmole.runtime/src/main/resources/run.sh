#!/bin/sh

MEMORY=$1
shift
java -Xmx${MEMORY} -jar plugins/org.eclipse.equinox.launcher.jar $@ 
