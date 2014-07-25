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

ulimit -v unlimited
ulimit -s unlimited

java -Xmx${MEMORY} -Dosgi.locking=none -Dosgi.configuration.area=${CONFIGDIR} $FLAG -XX:MaxMetaspaceSize=128m -XX:CompressedClassSpaceSize=128m -XX:+UseG1GC -XX:ParallelGCThreads=1 -jar plugins/org.eclipse.equinox.launcher.jar -consoleLog $@ 

RETURNCODE=$?

rm -rf ${CONFIGDIR}

exit $RETURNCODE

