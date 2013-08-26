#!/bin/bash

## 11 August 2013
## ============================
## JX Verification Manager Test
## ============================
## This script compiles and runs the JX verification test of the session maneger.

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
rm -rf JXLog.txt*
rm -rf *.log
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
javac -cp "jx.jar:." *.java

# Run the tests
java -classpath jx.jar:. TestManager $IP $PORT $TEST_NUMBER
