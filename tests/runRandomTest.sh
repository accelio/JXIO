#!/bin/bash

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TOP_DIR=$DIR/../
cd $DIR

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
        echo -e "\n[ERROR] JAVA_HOME not defined in shell!\n"
        exit 1
fi

# Compile
echo -e "\nCompiling JAVA files...."
javac -cp "$TOP_DIR/bin/jxio.jar:$TOP_DIR/src/lib/commons-logging.jar" `find -name "*.java"`
if [[ $? != 0 ]] ; then
    exit 1
fi

java_coverage_props=""
if [[ -n "$CODE_COVERAGE_ON" ]];then
	java_coverage_props="-D"$COBERTURA_COVFILE_PATH_PROP_NAME"="$COBERTURA_COVFILE
fi

# Run the tests
echo -e "\nRunning random test...."
java -Dlog4j.configuration=com/mellanox/jxio/tests/random/storyrunner/log4j.properties.randomtest -cp "$COBERTURA_JAR_PATH:$TOP_DIR/bin/jxio.jar:$TOP_DIR/src/lib/commons-logging.jar:$TOP_DIR/src/lib/log4j-1.2.15.jar:." $java_coverage_props com.mellanox.jxio.tests.random.Main $1 $2 $3 $4 &

# Check for timeout
if [ ! -z "$5" ]; then
        # Handle timeout
        sleep $5
        if [ `ps -ef | grep java | grep random | tr -s ' ' | cut -d " " -f 2 | wc -l` -eq 0 ]; then
                echo -e "\n[SUCCESS] Random test finished successfully!\n"
                exit 0
        fi
        echo -e "\n[TIMEOUT ERROR] Random test timed out!\n"
        for process in `ps -ef | grep java | grep random | tr -s ' ' | cut -d " " -f 2`; do
                sudo kill $process
        done
        exit 1
fi
