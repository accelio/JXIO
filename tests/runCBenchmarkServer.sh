#!/bin/bash

export LD_LIBRARY_PATH=../accelio/src/usr/.libs/
#$1 server IP
#$2 server port

taskset -c 1 com/mellanox/jxio/tests/benchmarks/xio_server $1 $2

