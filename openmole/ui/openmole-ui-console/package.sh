#!/bin/sh

VERSION="0.3-"`date +%Y%m%d%H%M%S` \
 && rm -Rf target && mkdir target \
 && cp -ar org.openmole.ui.console/target/product target/openmole-ui-console \
 && cp -ar plugins/target/dependency target/openmole-ui-console/openmole-plugins \
 && cp -ar plugins-ui/target/dependency target/openmole-ui-console/openmole-plugins-ui \
 && cp -a  ../../runtime/org.openmole.runtime/target/org.openmole.runtime-0.3.tar.bz2 target/openmole-ui-console/ \
 && cd target \
 && chmod +x openmole-ui-console/run.sh \
 && zip -r openmole-ui-console-$VERSION.zip openmole-ui-console

# if strange behaviour on our hudson server
#/openmole-plugins.dir target/product/openmole-plugins
