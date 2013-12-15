#!/bin/bash

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TOP_DIR="$DIR/../"
cd $DIR

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
        echo -e "\nError: JAVA_HOME not defined in shell!"
        exit 1
fi

# Compile
echo -e "\nCompiling JAVA files...."
javac -cp "$TOP_DIR/bin/jxio.jar:$TOP_DIR/src/lib/commons-logging.jar" ./com/mellanox/jxio/tests/*.java
if [[ $? != 0 ]] ; then
    exit 1
fi

java_coverage_props=""
if [[ -n "$CODE_COVERAGE_ON" ]];then
	java_coverage_props="-D"$COBERTURA_COVFILE_PATH_PROP_NAME"="$COBERTURA_COVFILE
fi

# Run the tests
echo -e "\nRunning random test...."
java -Dlog4j.configuration=com/mellanox/jxio/tests/log4j.properties.jxiotest -cp "$COBERTURA_JAR_PATH:$TOP_DIR/bin/jxio.jar:$TOP_DIR/src/lib/commons-logging.jar:$TOP_DIR/src/lib/log4j-1.2.15.jar:." $java_coverage_props com.mellanox.jxio.tests.BreakEventLoopTests
