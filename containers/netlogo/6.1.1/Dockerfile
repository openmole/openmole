FROM debian:testing

MAINTAINER "Romain Reuillon <romain.reuillon@openmole.org"
    
RUN apt-get update && \
    apt-get install --no-install-recommends -y ca-certificates default-jre-headless ca-certificates-java bash tar gzip locales wget && \
       apt-get clean autoclean && apt-get autoremove --yes && rm -rf /var/lib/{apt,dpkg,cache,log}/ /var/lib/apt/lists/* && \
       mkdir -p /lib/modules

ENV JAVA_HOME=/usr/lib/jvm/default-java
ARG NETLOGO_HOME=/netlogo
ARG NETLOGO_VERSION=6.1.1

ENV LC_ALL=C.UTF-8 \
    LANG=C.UTF-8 \
    NETLOGO_TARBALL=NetLogo-$NETLOGO_VERSION-64.tgz

ENV NETLOGO_URL=https://ccl.northwestern.edu/netlogo/$NETLOGO_VERSION/$NETLOGO_TARBALL

RUN wget $NETLOGO_URL && \ 
    tar xzf $NETLOGO_TARBALL && \
    ln -sf "NetLogo $NETLOGO_VERSION" netlogo && \
    rm -f $NETLOGO_TARBALL && \
    (cd $NETLOGO_HOME && cp netlogo-headless.sh netlogo-headless-limit.sh && sed 's/^\"\$\JAVA\"/cpulimit -l 100 -f -- \"\$JAVA\"/1' netlogo-headless-limit.sh)

ENV PATH=${NETLOGO_HOME}:${PATH}
