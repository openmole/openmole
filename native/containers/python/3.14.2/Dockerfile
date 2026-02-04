FROM python:3.14.2

LABEL maintainer="Romain Reuillon <romain.reuillon@iscpif.fr>"

RUN apt-get update \ 
	&& apt-get install -y --no-install-recommends fakeroot \
	&& rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* 

RUN cp /bin/fakeroot /bin/sudo 

