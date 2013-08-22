#!/bin/bash

## 11 August 2013
## ===========================
## JX Verification Client Test
## ===========================
## This script compiles and runs the JX verification test of the session client.

echo -e "\n******************* JX Verification Client Test *******************"

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
rm -rf /.autodirect/mtrswgwork/UDA/core_files_TEMP/*

# Get machine IP
IP="36.0.0.251"

# Configure Port
PORT=1234

# Get Test Number
TEST_NUMBER=$1

# Compile
javac -cp "jx.jar:." *.java

# Run the tests
java -classpath jx.jar:. TestClient $IP $PORT $TEST_NUMBER
