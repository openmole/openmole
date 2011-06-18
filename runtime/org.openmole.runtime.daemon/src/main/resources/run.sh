#!/bin/sh

java -Xmx64m -Dosgi.classloader.singleThreadLoads=true -jar plugins/org.eclipse.equinox.launcher.jar $@ 
