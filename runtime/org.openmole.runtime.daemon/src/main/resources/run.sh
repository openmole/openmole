#!/bin/sh

java -Xmx92m -Dosgi.classloader.singleThreadLoads=true -XX:+UseCompressedOops -XX:+CMSClassUnloadingEnabled -XX:+UseParallelGC -jar plugins/org.eclipse.equinox.launcher.jar $@ 
