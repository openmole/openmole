#!/bin/sh

MEMORY=$1
shift

CONFIGDIR=$1
shift

FLAG=""

JVMVERSION=`java -version 2>&1 | tail -1 -`

case "$JVMVERSION" in
  *64-Bit*) FLAG="-XX:+UseCompressedOops";;
esac


cp -r configuration ${CONFIGDIR}

java -Xmx${MEMORY} -Dosgi.locking=none -Dosgi.configuration.area=${CONFIGDIR} $FLAG -XX:MaxPermSize=128M -XX:+UseParallelGC -jar plugins/org.eclipse.equinox.launcher.jar $@ 

rm -rf ${CONFIGDIR}

