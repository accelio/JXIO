#!/bin/bash

export LD_LIBRARY_PATH=../accelio/src/usr/.libs/

#$1 server IP
#$2 server port
#$3 num sessions 

taskset -c 1 com/mellanox/jxio/tests/benchmarks/xio_client_stat $1 $2 $3

