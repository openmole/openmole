#!/usr/bin/env sh

#readlink -f does not work on mac, use alternate script
TARGET_FILE="$0"

cd `dirname "$TARGET_FILE"`
TARGET_FILE=`basename "$TARGET_FILE"`

# Iterate down a (possible) chain of symlinks
while [ -L "$TARGET_FILE" ]
do
    TARGET_FILE=`readlink "$TARGET_FILE"`
    cd `dirname "$TARGET_FILE"`
    TARGET_FILE=`basename "$TARGET_FILE"`
done

REALPATH="$TARGET_FILE"
#end of readlink -f

LOCATION=$( cd $(dirname "$REALPATH") ; pwd -P )

MEMORY=$1
shift

TMPDIR=$1
shift
mkdir -p "${TMPDIR}"

FLAG=""

JVMVERSION=`java -version 2>&1 | tail -1 -`

case "$JVMVERSION" in
  *64-Bit*) FLAG="-XX:+UseCompressedOops";;
esac

for a in $@
do
  if [[ $a == "--debug" ]]; then FLAG="$FLAG -XX:-OmitStackTraceInFastThrow"; fi
done


CONFIGDIR="${TMPDIR}/config"
mkdir -p "${CONFIGDIR}"

ulimit -S -v unlimited
ulimit -S -s unlimited

export MALLOC_ARENA_MAX=1

export LC_ALL="en_US.UTF-8"
export LANG="en_US.UTF-8"

java -Djava.io.tmpdir="${TMPDIR}" -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Xss2M -Xms64m -Xmx${MEMORY} -Dosgi.locking=none -Dosgi.configuration.area="${CONFIGDIR}" $FLAG -XX:ReservedCodeCacheSize=128m -XX:MaxMetaspaceSize=256m -XX:CompressedClassSpaceSize=128m \
  -XX:+UseG1GC -XX:ParallelGCThreads=1 -XX:CICompilerCount=2 -XX:ConcGCThreads=1 -XX:G1ConcRefinementThreads=1 \
  -cp "${LOCATION}/launcher/*" org.openmole.launcher.Launcher --plugins "${LOCATION}/plugins/" --priority "logging" --run org.openmole.runtime.SimExplorer --osgi-directory "${CONFIGDIR}" -- --workspace "${TMPDIR}" $@

RETURNCODE=$?

rm -rf "${TMPDIR}"

exit $RETURNCODE

