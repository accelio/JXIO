#!/bin/bash

###########################
###    December 2013    ###
### JXIO Timeout Script ###
################################################################
# This script recieves a single argument which indicates the   #
# amount in SECONDS after which the kill all script is called. #
# If given no argument this script does nothing.               #
################################################################

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Check for timeout
if [ ! -z "$1" ]; then
	echo "[TIMEOUT ENABLED] $1 seconds."
        # Handle timeout
        let "timeout=0"
        while [ $timeout -lt $1 ]; do
                sleep 1
                let "timeout+=1"
                # Check if test process ended
                if [ `ps -ef | grep java | grep random | tr -s ' ' | cut -d " " -f 2 | wc -l` -eq 0 ]; then
                        exit 0
                fi
        done
        echo -e "\n[TIMEOUT ERROR] Random test timed out!\n"
        # Kill All
        bash $DIR/killall.sh
        exit 1
fi
