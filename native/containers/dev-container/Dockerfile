
FROM ubuntu:22.04

USER root

#RUN echo "deb http://ftp.de.debian.org/debian jessie-backports main\n" >>/etc/apt/sources.list 
#RUN apt update && apt install -y apt-transport-https gnupg2
#RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
#RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
ENV DEBIAN_FRONTEND=noninteractive
RUN apt update && apt install -y openjdk-17-jdk curl gnupg2 apt-transport-https && apt-get clean

#RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
#RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
#RUN curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | bash

RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
RUN chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg

RUN apt update && apt install -y npm nodejs sbt git git-lfs figlet unzip && apt-get clean

RUN curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh && apt-get clean

ARG USER_NAME="openmole"
ARG UID=1000
ARG GID=1000

RUN addgroup --gid $GID $USER_NAME
RUN useradd -m -u $UID -g $GID -o -s /bin/bash $USER_NAME
RUN usermod -g docker $USER_NAME 

#USER $UNAME


#RUN mkdir /sources/ && chmod 777 /sources/
#RUN chmod +x /usr/local/*
#WORKDIR /sources/

