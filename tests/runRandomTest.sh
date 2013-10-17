#!/bin/bash

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
        echo -e "\nError: JAVA not found!\n"
        exit 1
fi

# Compile
echo -e "\nCompiling JAVA files....\n"
javac -cp "../bin/jxio.jar:../lib/commons-logging.jar" ./com/mellanox/jxio/tests/random/storyrunner/*.java
if [[ $? != 0 ]] ; then
    exit 1
fi

# Run the tests
echo -e "\nRunning manager test....\n"
java -Dlog4j.configuration=com/mellanox/jxio/tests/random/storyrunner/log4j.properties.randomtest -cp "../bin/jxio.jar:../lib/commons-logging.jar:../lib/log4j-1.2.15.jar:." com.mellanox.jxio.tests.random.storyrunner.StoryRunner
