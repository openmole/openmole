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

ulimit -S -v unlimited
ulimit -S -s unlimited

export MALLOC_ARENA_MAX=1

export LC_ALL="en_US.UTF-8"
export LANG="en_US.UTF-8"

java -Dfile.encoding=UTF-8 -Xms64m -Xmx${MEMORY} -Dosgi.locking=none -Dosgi.configuration.area=${CONFIGDIR} $FLAG -XX:ReservedCodeCacheSize=128m -XX:MaxMetaspaceSize=128m -XX:CompressedClassSpaceSize=128m -XX:+UseG1GC -XX:ParallelGCThreads=1 -jar plugins/org.eclipse.equinox.launcher.jar -consoleLog $@

RETURNCODE=$?

rm -rf ${CONFIGDIR}

exit $RETURNCODE

