FROM debian:testing-slim as build

MAINTAINER "Romain Reuillon <romain.reuillon@openmole.org"
    
#RUN apt-get update && \
#    apt-get install --no-install-recommends -y ca-certificates default-jre-headless ca-certificates-java bash tar gzip locales wget && \
#       apt-get clean autoclean && apt-get autoremove --yes && rm -rf /var/lib/{apt,dpkg,cache,log}/ /var/lib/apt/lists/* && \
#       mkdir -p /lib/modules

RUN apt-get update \ 
	&& apt-get install -y --no-install-recommends locales ca-certificates fakeroot sudo curl \
	&& rm -rf /var/lib/apt/lists/*

#ENV JAVA_HOME=/usr/lib/jvm/default-java

RUN curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh
RUN mv /root/.cache/scalacli/local-repo/bin/scala-cli/scala-cli /bin/

ARG NETLOGO_HOME=/netlogo
ARG NETLOGO_VERSION=7.0.0

ENV LC_ALL=C.UTF-8 \
    LANG=C.UTF-8 \
    NETLOGO_TARBALL=NetLogo-$NETLOGO_VERSION-64.tgz

ENV NETLOGO_URL=https://ccl.northwestern.edu/netlogo/$NETLOGO_VERSION/$NETLOGO_TARBALL

RUN curl -o $NETLOGO_TARBALL $NETLOGO_URL && \ 
    tar xzf $NETLOGO_TARBALL && \
    mv "`find . -maxdepth 1 -type d  -name "NetLogo*"`" netlogo && \
    rm -rf "$NETLOGO_HOME/lib/runtime" && \
    rm -rf "$NETLOGO_HOME/models"

COPY ./headless /headless
RUN (cd /headless && ./assemble.sh)

RUN curl -fL "https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz" | gzip -d > /bin/cs 
RUN chmod +x /bin/cs
RUN cd headless && ./fetch.sh

FROM debian:testing-slim

RUN apt-get update \
        && apt-get install -y --no-install-recommends locales ca-certificates fakeroot sudo wget \
        && rm -rf /var/lib/apt/lists/*

RUN wget -O jre.tar.gz https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.7%2B7/OpenJDK17U-jre_x64_linux_hotspot_17.0.7_7.tar.gz && \
    tar -xvzf jre.tar.gz && \
    rm jre.tar.gz && \
    mv jdk* jre

ENV PATH=/jre/bin:$PATH

ARG NETLOGO_HOME=/netlogo
ARG NETLOGO_VERSION=7.0.0

COPY --from=build $NETLOGO_HOME $NETLOGO_HOME
COPY --from=build /headless /headless


COPY ./netlogo-headless /bin/netlogo-headless
RUN java -cp /headless/init.jar Init

#RUN touch run.sc && scala-cli run --server=false -j 17 --dep "com.thoughtworks.xstream:xstream:1.4.20" run.sc && rm run.sc

#WORKDIR $NETLOGO_HOME
    
#(cd $NETLOGO_HOME && cp netlogo-headless.sh netlogo-headless-limit.sh && sed 's/^\"\$\JAVA\"/cpulimit -l 100 -f -- \"\$JAVA\"/1' netlogo-headless-limit.sh)

#ENV PATH=${NETLOGO_HOME}:${PATH}

