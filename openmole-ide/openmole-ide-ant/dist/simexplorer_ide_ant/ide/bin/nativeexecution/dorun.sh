#!/bin/sh 
PATH=/bin:/usr/bin
PROG=`basename "$0"`
USAGE="usage: ${PROG} [-p prompt] -x execScript"
PROMPT=NO
STATUS=-1

fail() {
  echo $@ >&2
  exit 1
}

doExit() {
  echo ${STATUS} > "${SHFILE}.res"
  exit ${STATUS}
}

[ $# -lt 1 ] && fail $USAGE

while getopts p:x: opt; do
  case $opt in
    x) SHFILE=$OPTARG
       ;;
    p) PROMPT=$OPTARG
       ;;
  esac
done

shift `expr $OPTIND - 1`

trap "doExit" 1 2 15 EXIT

sh "${SHFILE}"
STATUS=$?

echo ${STATUS} > "${SHFILE}.res"

if [ "${PROMPT}" != "NO" ]; then
  echo "${PROMPT}"
  read X
fi

