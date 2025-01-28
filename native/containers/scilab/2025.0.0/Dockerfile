## Emacs, make this -*- mode: sh; -*-
 
FROM debian:testing-slim

MAINTAINER "Romain Reuillon"

## Set a default user. Available via runtime flag `--user docker` 
## Add user to 'staff' group, granting them write privileges to /usr/local/lib/R/site.library
## User should also have & own a home directory (for rstudio or linked volumes to work properly). 
RUN useradd docker \
	&& mkdir /home/docker \
	&& chown docker:docker /home/docker \
	&& usermod -a -G staff docker # docker staff

RUN apt-get update \ 
	&& apt-get install -y --no-install-recommends ed less locales vim-tiny wget ca-certificates fonts-texgyre libtinfo6 xz-utils fakeroot sudo \
	&& rm -rf /var/lib/apt/lists/*

## Configure default locale, see https://github.com/rocker-org/rocker/issues/19
RUN echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
	&& locale-gen en_US.utf8 \
	&& /usr/sbin/update-locale LANG=en_US.UTF-8 \
        #&& ln -s /etc/locale.alias /usr/share/locale/locale.alias \
        && apt install locales

ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8

## Use Debian unstable via pinning -- new style via APT::Default-Release
#RUN echo "deb http://http.debian.net/debian sid main" > /etc/apt/sources.list.d/debian-unstable.list \
#	&& echo 'APT::Default-Release "testing";' > /etc/apt/apt.conf.d/default


#RUN apt-get update \
#	&& apt-get install -y \
#		scilab \
#	&& rm -rf /var/lib/apt/lists/*

ENV SCILAB_VERSION 2025.0.0
RUN wget https://www.scilab.org/download/${SCILAB_VERSION}/scilab-${SCILAB_VERSION}.bin.x86_64-linux-gnu.tar.xz -O scilab.tar.xz && \
    xz -d scilab.tar.xz && \
    tar -xvf scilab.tar && \
    ln -s $PWD/scilab-${SCILAB_VERSION}/bin/scilab /usr/bin/scilab && \
    ln -s $PWD/scilab-${SCILAB_VERSION}/bin/scilab-cli /usr/bin/scilab-cli && \
    rm scilab.tar

CMD ["scilab-cli"]
