#!/usr/bin/env bash

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

FULL_TMPDIR=`realpath ${TMPDIR}`
HOME_DIRECTORY=${FULL_TMPDIR}

ARGS=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --home-directory ) HOME_DIRECTORY="$2" ; shift ;;
    * ) ARGS="$ARGS $1";;
  esac
  shift
done
echo $ARGS

FLAG=""

JVMVERSION=`java -version 2>&1 | tail -1 -`

case "$JVMVERSION" in
  *64-Bit*) FLAG="-XX:+UseCompressedOops";;
esac

for a in $@
do
  if [ $a = "--debug" ]; then FLAG="$FLAG -XX:-OmitStackTraceInFastThrow"; fi
done


OSGI_CONFIGDIR="${FULL_TMPDIR}/osgi_config"
mkdir -p "${OSGI_CONFIGDIR}"

OPENMOLE_WORKSPACE="${FULL_TMPDIR}/openmole"
mkdir -p "${OPENMOLE_WORKSPACE}"

ulimit -S -v unlimited
ulimit -S -s unlimited

# Constrain JVM memory
export MALLOC_ARENA_MAX=1

# Set UTF-8 local
if [ `locale -a | grep C.UTF-8` ]; then OM_LOCAL="C.UTF-8"
elif [ `locale -a | grep en_US.utf8` ]; then OM_LOCAL="en_US.utf8"
elif [ `locale -a | grep utf8` ]; then OM_LOCAL=`locale -a |  grep utf8 | head -1`
else
  echo "No UTF8 locale found among installed locales"
  locale -a
fi

if [ -n "$OM_LOCAL" ]; then
  export LC_ALL=$OM_LOCAL
  export LANG=$OM_LOCAL
fi

## Just to be sure
export _JAVA_OPTIONS="-Duser.home=\"${HOME_DIRECTORY}\" -Djava.io.tmpdir=\"${FULL_TMPDIR}\""

java -Djava.io.tmpdir="${FULL_TMPDIR}" -Duser.home="${HOME_DIRECTORY}" -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Xss2M -Xms64m -Xmx${MEMORY} -Dosgi.configuration.area="${OSGI_CONFIGDIR}" -Djdk.util.zip.disableZip64ExtraFieldValidation=true $FLAG -XX:ReservedCodeCacheSize=128m -XX:MaxMetaspaceSize=256m -XX:CompressedClassSpaceSize=128m \
  -XX:+UseG1GC -XX:ParallelGCThreads=1 -XX:CICompilerCount=2 -XX:ConcGCThreads=1 -XX:G1ConcRefinementThreads=1 -XX:+UseStringDeduplication \
  --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.nio.file=ALL-UNNAMED \
  -cp "${LOCATION}/launcher/*" org.openmole.launcher.Launcher --plugins "${LOCATION}/plugins/" --priority "logging" --run org.openmole.runtime.SimExplorer --osgi-directory "${OSGI_CONFIGDIR}" --osgi-locking-none -- --workspace "${OPENMOLE_WORKSPACE}" $ARGS

RETURNCODE=$?

rm -rf "${TMPDIR}"

exit $RETURNCODE

