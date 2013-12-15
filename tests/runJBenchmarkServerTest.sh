#!/bin/bash

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

# Exporting LD library
export LD_LIBRARY_PATH=$DIR
echo -e "\nLD library is: $LD_LIBRARY_PATH"

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
	echo -e "\nERROR: JAVA not found!"
	exit 1
fi

# Check arguments
if [ -z $1 ]; then
	echo -e "$0 ERROR: Missig first parameter. Should be server IP.\n"
	exit 1
fi
if [ -z $2 ]; then
	echo -e "$0 ERROR: Missig second parameter. Should be a port number.\n"
	exit 1
fi
if [ -z $3 ]; then
	echo -e "$0 ERROR: Missig third parameter. Should be test type (w,r,l) .\n"
	exit 1
fi
if [ -z $4 ]; then
	echo -e "$0 ERROR: Missig forth parameter. Should be the message size .\n"
	exit 1
fi
if [ -z $5 ]; then
	echo -e "$0 ERROR: Missig fifth parameter. Should be the core number for 'taskset' command.\n"
	exit 1
fi


# Compile
echo -e "\nCompiling JAVA files...."
javac -cp "../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." com/mellanox/jxio/tests/benchmarks/DataPathTestMngServer.java com/mellanox/jxio/tests/benchmarks/DataPathTestServer.java
if [[ $? != 0 ]] ; then
    exit 1
fi

#The Below Server is for DataPath perfromance test
#$1 Server IP
#$2 Server port   
#$3 Test type ('w' for write test or 'r' for read test or 'l' latency test)
#$4 msg size
#$5 core number for 'taskset' command 

taskset -c $5 java -Dserver=1 -Dlog4j.configuration=com/mellanox/jxio/tests/log4j.properties.jxiotest -cp "../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." com.mellanox.jxio.tests.benchmarks.DataPathTestMngServer $1 $2 $3 $4 
