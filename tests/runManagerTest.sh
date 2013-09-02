#!/bin/bash

## ============================
## JXIO Verification Manager Test
## ============================
## This script compiles and runs the JXIO verification test of the session manager.

echo -e "\n******************* JXIO Verification Manager Test *******************"

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
        echo -e "\nError: JAVA not found!\n"
        exit 1
fi
# Checks to see if LD library path is valid
if [ ! -e ${LD_LIBRARY_PATH} ]; then
        echo -e "\nError: LD_LIBRAry not found!\n"
        exit 1
fi

# Remove temporary files and cores
rm -f bla
rm -f JXIOLog.txt*
rm -rf /.autodirect/mtrswgwork/UDA/core_files_TEMP/*

# Check arguments
if [ -z $1 ]; then
	 echo -e "\nError: Missig first parameter. Should be a hostname or IP.\n"
fi
if [ -z $2 ]; then
	 echo -e "\nError: Missig second parameter. Should be a port.\n"
fi
if [ -z $3 ]; then
	 echo -e "\nError: Missig third parameter. Should be a a test number.\n"
fi
# Get machine IP
IP=$1
# Configure Port
PORT=$2
# Get Test Number
TEST_NUMBER=$3

# Compile
echo -e "\nCompiling JAVA files....\n"
javac -cp "jx.jar:." ./*.java

# Run the tests
echo -e "\nRunning manager test....\n"
java -classpath jx.jar:. TestManager $IP $PORT $TEST_NUMBER
