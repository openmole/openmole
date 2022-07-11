
FROM ubuntu:20.04

USER root

#RUN echo "deb http://ftp.de.debian.org/debian jessie-backports main\n" >>/etc/apt/sources.list 
#RUN apt update && apt install -y apt-transport-https gnupg2
#RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
#RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
ENV DEBIAN_FRONTEND=noninteractive
RUN apt update && apt install -y openjdk-11-jdk curl gnupg2 apt-transport-https && apt-get clean

RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | bash

RUN apt update && apt install -y npm nodejs sbt git git-lfs figlet unzip && apt-get clean

RUN curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh && apt-get clean

RUN (curl https://rclone.org/install.sh | bash) && apt-get clean

RUN mkdir /sources/ && chmod 777 /sources/
COPY ./scripts/clone /usr/local/bin
COPY ./scripts/compile /usr/local/bin
COPY ./scripts/openmole-docker /usr/local/bin
RUN chmod +x /usr/local/*
WORKDIR /sources/

RUN cd /sources && /usr/local/bin/clone
RUN /usr/local/bin/compile
RUN ln -s /sources/openmole/openmole/bin/openmole/target/assemble/openmole /usr/local/bin/openmole
RUN chmod a+x /usr/local/bin/openmole-docker

CMD openmole-docker

