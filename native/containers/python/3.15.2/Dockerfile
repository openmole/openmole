FROM python:3.15.2

LABEL maintainer="Romain Reuillon <romain.reuillon@iscpif.fr>"

RUN apt-get update \ 
	&& apt-get install -y --no-install-recommends fakeroot \
	&& rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* 

RUN cp /bin/fakeroot /bin/sudo 

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

#RUN curl -O https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh && \
#    bash Miniconda3-latest-Linux-x86_64.sh -b -p /opt/conda && \
#    rm Miniconda3-latest-Linux-x86_64.sh

#ENV PATH="/opt/conda/bin:${PATH}"
