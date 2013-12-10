#!/bin/bash

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
        echo -e "\nError: JAVA_HOME not defined in shell!\n"
        exit 1
fi

# Compile
echo -e "\nCompiling JAVA files....\n"
javac -cp "../bin/jxio.jar:../src/lib/commons-logging.jar" `find -name "*.java"`
if [[ $? != 0 ]] ; then
    exit 1
fi

java_coverage_props=""
if [[ -n "$CODE_COVERAGE_ON" ]];then
	java_coverage_props="-D"$COBERTURA_COVFILE_PATH_PROP_NAME"="$COBERTURA_COVFILE
fi

# Run the tests
echo -e "\nRunning random test....\n"
java -Dlog4j.configuration=com/mellanox/jxio/tests/random/storyrunner/log4j.properties.randomtest -cp "$COBERTURA_JAR_PATH:../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." $java_coverage_props com.mellanox.jxio.tests.random.Main $1 $2 $3 $4
