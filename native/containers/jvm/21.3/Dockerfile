## Emacs, make this -*- mode: sh; -*-
 
FROM debian:testing-slim

## Set a default user. Available via runtime flag `--user docker` 
## Add user to 'staff' group, granting them write privileges to /usr/local/lib/R/site.library
## User should also have & own a home directory (for rstudio or linked volumes to work properly). 
RUN useradd user \
	&& mkdir /home/user \
	&& chown user:user -R /home/user 
	#&& usermod -a -G staff user # docker staff

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

ENV LC_ALL=en_US.UTF-8
ENV LANG=en_US.UTF-8
ENV XDG_CACHE_HOME=/home/user/.cache

USER user

RUN curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh

USER root
RUN cp /home/user/.cache/scalacli/local-repo/bin/scala-cli/scala-cli /bin/

USER user
RUN cd /tmp && touch run.sc && scala-cli run run.sc --server=false -j 21 --dep "com.thoughtworks.xstream:xstream:1.4.21" && rm run.sc
#RUN mv /root /home/user && mkdir /root


