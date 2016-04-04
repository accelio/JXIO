#!/bin/bash

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

# Exporting LD library
export LD_LIBRARY_PATH=$DIR
echo -e "\nLD library is: $LD_LIBRARY_PATH"

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
	echo -e "\nERROR: JAVA not found!\n"
	exit 1
fi

# Check and get arguments
if [ -z $1 ]; then
	echo -e "$0 ERROR: Missig first parameter. Should be a 'server' or 'client'.\n"
	exit 1
fi
SIDE=$1 # Get server or client side
if [ -z $2 ]; then
	echo -e "$0 ERROR: Missig second parameter. Should be a server IP.\n"
	exit 1
fi
IP=$2 # Get machine IP
if [ -z $3 ]; then
	echo -e "$0 ERROR: Missig third parameter. Should be a server port.\n"
	exit 1
fi
PORT=$3 # Get Port
if [ -z $4 ]; then
        echo -e "\n$0 WARING: no number of requests given, using default: 600000."
	NUMBER_OF_REQS=600000
else
	NUMBER_OF_REQS=$4 # Get number of reqs (optional)
fi
if [ -z $5 ]; then
        echo -e "$0 WARING: no print counter given, using default: 10000.\n"
	PRINT_COUNTER=10000
else
	PRINT_COUNTER=$5 # Get print counter (optional)
fi

# Compile
echo -e "\nCompiling JAVA files...."
javac -cp "../bin/jxio.jar:../src/lib/commons-logging.jar" org/accelio/jxio/simpletest/*.java
if [[ $? != 0 ]] ; then
    exit 1
fi

# Run the tests
export LD_LIBRARY_PATH=$DIR
if ([ $SIDE == server ]); then
APPLICATION_NAME="Server"
APPLICATION="org.accelio.jxio.simpletest.SimpleServer"
elif ([ $SIDE == client ]); then
APPLICATION_NAME="Client"
APPLICATION="org.accelio.jxio.simpletest.SimpleClient"
else
echo -e "$0 ERROR: Missig first parameter. Should be a 'server' or 'client'.\n"
exit 1
fi

# Config Covertura
java_coverage_props=""
if [[ -n "$CODE_COVERAGE_ON" ]];then
        java_coverage_props="-D"$COBERTURA_COVFILE_PATH_PROP_NAME"="$COBERTURA_COVFILE
fi

# Run the tests
echo -e "\nRunning ${APPLICATION_NAME} side test..."
java -Dlog4j.configuration=org/accelio/jxio/log4j.properties.jxio -cp "$COBERTURA_JAR_PATH:../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." $java_coverage_props $APPLICATION $IP $PORT $NUMBER_OF_REQS $PRINT_COUNTER
