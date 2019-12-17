
FROM ubuntu:18.04

USER root

#RUN echo "deb http://ftp.de.debian.org/debian jessie-backports main\n" >>/etc/apt/sources.list 
#RUN apt update && apt install -y apt-transport-https gnupg2
#RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
#RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
RUN apt update && apt install -y openjdk-11-jdk curl npm nodejs apt-transport-https gnupg2

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get update && apt-get install -y sbt

RUN curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | bash
RUN apt update && apt install -y git git-lfs 

RUN mkdir /sources/ && chmod 777 /sources/
COPY ./scripts/clone /usr/local/bin
COPY ./scripts/compile /usr/local/bin
RUN chmod +x /usr/local/*
WORKDIR /sources/

