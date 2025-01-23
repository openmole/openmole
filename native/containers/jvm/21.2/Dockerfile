## Emacs, make this -*- mode: sh; -*-
 
FROM debian:testing-slim

MAINTAINER "Romain Reuillon"

## Set a default user. Available via runtime flag `--user docker` 
## Add user to 'staff' group, granting them write privileges to /usr/local/lib/R/site.library
## User should also have & own a home directory (for rstudio or linked volumes to work properly). 
#RUN useradd docker \
#	&& mkdir /home/docker \
#	&& chown docker:docker /home/docker \
#	&& usermod -a -G staff docker # docker staff

RUN apt-get update \ 
	&& apt-get install -y --no-install-recommends locales ca-certificates fakeroot sudo curl \
	&& rm -rf /var/lib/apt/lists/* \
        && rm -rf /var/lib/{apt,dpkg,cache,log}/

## Configure default locale, see https://github.com/rocker-org/rocker/issues/19
RUN echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
	&& locale-gen en_US.utf8 \
	&& /usr/sbin/update-locale LANG=en_US.UTF-8 \
        #&& ln -s /etc/locale.alias /usr/share/locale/locale.alias \
        && apt install locales

ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8

RUN curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh
RUN cp /root/.cache/scalacli/local-repo/bin/scala-cli/scala-cli /bin/

RUN cd /tmp && touch run.sc && scala-cli run run.sc --server=false -j 21 --dep "com.thoughtworks.xstream:xstream:1.4.21" && rm run.sc
RUN mv /root /home/user && mkdir /root


