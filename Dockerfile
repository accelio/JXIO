FROM java:8-jdk

RUN apt-get update && \
    apt-get install -y \
        build-essential \
	    libtool \
	    autoconf \
	    git \
	    libnuma-dev \
	    librdmacm-dev && \
    rm -rf /var/lib/apt/lists/*

