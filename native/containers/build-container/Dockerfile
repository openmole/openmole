
FROM debian:testing

USER root

#RUN echo "deb http://ftp.de.debian.org/debian jessie-backports main\n" >>/etc/apt/sources.list 
#RUN apt update && apt install -y apt-transport-https gnupg2
#RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
#RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
ENV DEBIAN_FRONTEND=noninteractive

#RUN sed -i '/security/d' /etc/apt/sources.list \
#        && apt-get update -y \
#	&& apt-get install -y wget \
#	&& wget -O netselect.deb http://http.us.debian.org/debian/pool/main/n/netselect/netselect_0.3.ds1-28+b1_`dpkg --print-architecture`.deb \
#	&& dpkg -i netselect.deb \
#	&& rm netselect.deb \
#	&& sed -r -i -e "s#http://(archive|security)\.ubuntu\.com/ubuntu/?#$(netselect -v -s1 -t20 `wget -q -O- https://launchpad.net/ubuntu/+archivemirrors | grep -P -B8 "statusUP|statusSIX" | grep -o -P "http://[^\"]*"`|grep -P -o 'http://.+$')#g" /etc/apt/sources.list 

RUN apt update && apt install -y openjdk-17-jdk curl gnupg2 apt-transport-https && apt-get clean

#RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
#RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
#RUN curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | bash

RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
RUN chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg

RUN apt update && apt install -y npm nodejs sbt git git-lfs figlet unzip docker.io && apt-get clean

#RUN curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh && apt-get clean

RUN (curl https://rclone.org/install.sh | bash) && apt-get clean

RUN mkdir /sources/ && chmod 777 /sources/
COPY ./scripts/clone /usr/local/bin
COPY ./scripts/compile /usr/local/bin
RUN chmod +x /usr/local/*
WORKDIR /sources/

