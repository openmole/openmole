#!/bin/sh

VERSION="0.3-"`date +%Y%m%d%H%M%S` \
 && rm -Rf target && mkdir target \
 && cp -ar org.openmole.ui.console/target/product target/openmole-ui-console-$VERSION \
 && cp -ar plugins/target/dependency target/openmole-ui-console-$VERSION/openmole-plugins \
 && cp -ar plugins-ui/target/dependency target/openmole-ui-console-$VERSION/openmole-plugins-ui \
 && cd target \
 && chmod +x openmole-ui-console-$VERSION/run.sh \
 && zip -r openmole-ui-console-$VERSION.zip openmole-ui-console-$VERSION

# if strange behaviour on our hudson server
#/openmole-plugins.dir target/product/openmole-plugins
