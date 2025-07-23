## Emacs, make this -*- mode: sh; -*-
 
FROM rocker/r2u:24.04

LABEL maintainer="Romain Reuillon <romain.reuillon@iscpif.fr>; Juste Raimbault <juste.raimbault@ign.fr>"

## Set a default user. Available via runtime flag `--user docker` 
## Add user to 'staff' group, granting them write privileges to /usr/local/lib/R/site.library
## User should also have & own a home directory (for rstudio or linked volumes to work properly). 
#RUN useradd docker \
#	&& mkdir /home/docker \
#	&& chown docker:docker /home/docker \
#	&& adduser docker staff

RUN apt-get update \ 
	&& apt-get install -y --no-install-recommends \
		locales \
		wget \
		ca-certificates \
                fakeroot \
	&& rm -rf /var/lib/apt/lists/* 

## Configure default locale, see https://github.com/rocker-org/rocker/issues/19
RUN echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
	&& locale-gen en_US.utf8 \
	&& /usr/sbin/update-locale LANG=en_US.UTF-8

ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8

## Use Debian unstable via pinning -- new style via APT::Default-Release
#RUN echo "deb http://http.debian.net/debian sid main" > /etc/apt/sources.list.d/debian-unstable.list \
#	&& echo 'APT::Default-Release "testing";' > /etc/apt/apt.conf.d/default

#ENV DEBIAN_FRONTEND=noninteractive

#RUN Rscript --vanilla -e 'install.packages("bspm", repos="https://cran.r-project.org")' 
RUN cp /bin/fakeroot /bin/sudo && \
      echo "options(bspm.sudo=TRUE)" | cat - /etc/R/Rprofile.site > /tmp/out && mv /tmp/out /etc/R/Rprofile.site && \
      R --slave -e 'install.packages(c("jsonlite", "remotes"))'

RUN rm -rf /var/lib/apt/lists/*

CMD ["R"]
