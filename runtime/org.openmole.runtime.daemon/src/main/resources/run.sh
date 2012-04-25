#!/bin/bash

FLAG=""

JVMVERSION=`java -version 2>&1 | tail -1 -`

case "$JVMVERSION" in
  *64-Bit*) FLAG="-XX:+UseCompressedOops";;
esac 


java -Xmx92m $FLAG -XX:+CMSClassUnloadingEnabled -XX:+UseParallelGC -jar plugins/org.eclipse.equinox.launcher.jar $@ 
