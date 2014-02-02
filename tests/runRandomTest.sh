#!/bin/bash

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TOP_DIR=$DIR/..
cd $DIR

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
        echo -e "\n[ERROR] JAVA_HOME not defined in shell!\n"
        exit 1
fi

# Compile
echo -e "\nCompiling JAVA files...."
javac -cp "$TOP_DIR/bin/jxio.jar:$TOP_DIR/src/lib/commons-logging.jar" `find $TOP_DIR/tests/com/mellanox/jxio/tests/random/ -name "*.java"`
if [[ $? != 0 ]] ; then
    exit 1
fi

java_coverage_props=""
if [[ -n "$CODE_COVERAGE_ON" ]];then
	java_coverage_props="-D"$COBERTURA_COVFILE_PATH_PROP_NAME"="$COBERTURA_COVFILE
fi

# Run the tests
let "run_stat=1"
echo -e "\nRunning random test...."
bash $TOP_DIR/cases/timeout.sh $4 "random" &
java -Dlog4j.configuration=com/mellanox/jxio/tests/random/storyrunner/log4j.properties.randomtest -cp "$COBERTURA_JAR_PATH:$TOP_DIR/bin/jxio.jar:$TOP_DIR/src/lib/commons-logging.jar:$TOP_DIR/src/lib/log4j-1.2.15.jar:." $java_coverage_props com.mellanox.jxio.tests.random.Main $1 $2 $3 $COBERTURA_JAR_PATH $java_coverage_props
