#!/bin/sh

java -Xmx92m -Dosgi.classloader.singleThreadLoads=true -jar plugins/org.eclipse.equinox.launcher.jar $@ 
