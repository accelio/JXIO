#!/bin/bash

############################
###     December 2013    ###
### JXIO Kill All Script ###
############################

echo "[KILLALL] Killing all JXIO processes!"
for process in `ps -ef | grep java | grep random | tr -s ' ' | cut -d " " -f 2`; do
                sudo kill $process
done
echo "[KILLALL] DONE!"
