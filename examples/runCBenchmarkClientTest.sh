#!/bin/bash

export LD_LIBRARY_PATH=../accelio/src/usr/.libs/

#$1 server IP
#$2 server port
#$3 num sessions 

#The below test measures connection establishment performance
taskset -c 1 org/accelio/jxio/tests/benchmarks/xio_client_stat $SERVER_IP $SERVER_PORT $NUM_SESSIONS

#The below test measures DataPath perfromance TPS & BW

taskset -c 1 ./accelio/tests/usr/hello_test/xio_server -p $SERVER_PORT -n 0 -w $MSG_SIZE $SERVER_IP 

