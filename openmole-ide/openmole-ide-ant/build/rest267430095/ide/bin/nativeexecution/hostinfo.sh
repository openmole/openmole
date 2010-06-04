#!/bin/sh

PATH=/usr/bin:/bin:$PATH
HOSTNAME=`uname -n`
OS=`uname -s`
CPUTYPE=`uname -p`
BITNESS=32

LS=/bin/ls
OSFAMILY=
DATETIME=`date -u +'%Y-%m-%d %H:%M:%S'`

if [ "${CPUTYPE}" = "unknown" ]; then
   CPUTYPE=`uname -m`
fi

if [ "${OS}" = "SunOS" ]; then
   BITNESS=`isainfo -b`
   OSFAMILY="SUNOS"
   OSNAME="SunOS"
   OSBUILD=`head -1 /etc/release | sed -e "s/^ *//"`
   CPUNUM=`/usr/sbin/psrinfo -v | grep "^Status of" | wc -l | sed 's/^ *//'`
else
   if [ "${OS}" = "Darwin" ]; then
      sysctl hw.cpu64bit_capable | grep -q "1$"
      if [ $? -eq 0 ]; then
         BITNESS=64
      fi
   else
      uname -a | egrep "x86_64|WOW64" >/dev/null
      if [ $? -eq 0 ]; then
         BITNESS=64
      fi
   fi

   if [ -f "/etc/sun-release" ]; then
      OSNAME="${OS}-JDS"
      OSBUILD=`head -1 /etc/sun-release`
   elif [ -f /etc/SuSE-release ]; then
      OSNAME="${OS}-SuSE"
      OSBUILD=`cat /etc/SuSE-release | tr "\n" " "`;
   elif [ -f /etc/redhat-release ]; then
      OSNAME="${OS}-Redhat"
      OSBUILD=`head -1 /etc/redhat-release`
   elif [ -f /etc/gentoo-release ]; then
      OSNAME="${OS}-Gentoo"
      OSBUILD=`head -1 /etc/gentoo-release`
   elif [ -f /etc/lsb-release ]; then
      OSNAME="${OS}-"`cat /etc/lsb-release | grep DISTRIB_ID | sed 's/.*=//'`
      OSBUILD=`cat /etc/lsb-release | grep DISTRIB_DESCRIPTION | sed 's/.*=//' | sed 's/"//g'`
   fi
fi

OSFAMILY=${OSFAMILY:-`echo ${OS} | grep _NT- >/dev/null && echo WINDOWS`}
OSFAMILY=${OSFAMILY:-`test "$OS" = "Darwin" && echo MACOSX`}
OSFAMILY=${OSFAMILY:-`test "$OS" = "Linux" && echo LINUX`}
OSFAMILY=${OSFAMILY:-${OS}}

CPUFAMILY=`(echo ${CPUTYPE} | egrep "^i|x86_64|athlon|Intel" >/dev/null && echo x86) || echo ${CPUTYPE}`
USERDIRBASE=${HOME}

if [ "${OSFAMILY}" = "LINUX" ]; then
   CPUNUM=`cat /proc/cpuinfo | grep processor | wc -l | sed 's/^ *//'`
elif [ "${OSFAMILY}" = "WINDOWS" ]; then
   CPUNUM=$NUMBER_OF_PROCESSORS
   OSNAME=`uname`
   USERDIRBASE=${USERPROFILE}
elif [ "${OSFAMILY}" = "MACOSX" ]; then
   CPUNUM=`hostinfo | awk '/processor.*logical/{print $1}'`
   OSNAME="MacOSX"
   OSBUILD=`hostinfo | sed -n '/kernel version/{n;p;}' | sed 's/[	 ]*\([^:]*\).*/\1/'`
fi

USER=${USER:-`logname 2>/dev/null`}
USER=${USER:-${USERNAME}}
TMPBASE=${TMPBASE:-/var/tmp}
TMPDIRBASE=${TMPBASE}/dlight_${USER}/${NB_KEY}
mkdir -p "${TMPDIRBASE}"
ENVFILE="${TMPDIRBASE}/env"

echo BITNESS=${BITNESS}
echo CPUFAMILY=${CPUFAMILY}
echo CPUNUM=${CPUNUM}
echo CPUTYPE=${CPUTYPE}
echo HOSTNAME=${HOSTNAME}
echo OSNAME=${OSNAME}
echo OSBUILD=${OSBUILD}
echo OSFAMILY=${OSFAMILY}
echo USER=${USER}
echo SH=${SHELL}
echo USERDIRBASE=${USERDIRBASE}
echo TMPDIRBASE=${TMPDIRBASE}
echo DATETIME=${DATETIME}
echo ENVFILE=${ENVFILE}
exit 0
