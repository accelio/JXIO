#!/bin/bash

############################
###     December 2013    ###
### JXIO Kill All Script ###
############################

echo "[KILLALL] Killing all JXIO processes!"
echo "--- jps ---"
jps
echo "-----------"
echo "--- ps -ef / grep java / grep $1 ---"
ps -ef | grep java | grep $1
echo "----------------------------------------"
echo "--- jps ---"
jps
echo "-----------"
for process in `ps -ef | grep java | grep $1 | tr -s ' ' | cut -d " " -f 2`; do
                sudo kill -9 $process
done
echo "[KILLALL] DONE!"
