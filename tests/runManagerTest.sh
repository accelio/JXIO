#!/bin/bash

## ============================
## JX Verification Manager Test
## ============================
## This script compiles and runs the JX verification test of the session manager.

echo -e "\n******************* JX Verification Manager Test *******************"

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
rm -f JXLog.txt*
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
javac -cp "jx.jar:." Tests/*.java managerTests/*.java

# Run the tests
java -classpath jx.jar:. managerTests/TestManager $IP $PORT $TEST_NUMBER
