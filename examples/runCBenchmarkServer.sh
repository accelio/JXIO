#!/bin/bash

export LD_LIBRARY_PATH=../accelio/src/usr/.libs/

#Run the server below for connection establishment perfromance 

taskset -c 1 org/accelio/jxio/tests/benchmarks/xio_server $SERVER_IP $SERVER_PORT

#Run the server below for DataPathTest performance (TPS &BW)

taskset -c 1 ./accelio/tests/usr/hello_test/xio_server -p $SERVER_PORT -n 0 -w $MSG_SIZE $SERVER_IP
