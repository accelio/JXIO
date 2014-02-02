#!/bin/bash

# Configuring Running Directory
TOP_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $TOP_DIR
echo -e "\nThe JXIO top directory is $TOP_DIR\n"

TARGET=jxio.jar
BIN_FOLDER=$TOP_DIR/bin
LIB_FOLDER=$TOP_DIR/src/lib
SRC_JAVA_FOLDER=$TOP_DIR/src/java
SRC_JAVA_FILES="$SRC_JAVA_FOLDER/com/mellanox/jxio/*.java $SRC_JAVA_FOLDER/com/mellanox/jxio/impl/*.java"
NATIVE_LIBS="libjxio.so libxio.so"

export PATH=$BULLSEYE_DIR:$PATH
 #turning off bullseye for case it was left on (only if cov01 is found on this machine)
BULLSEYE_CMD=cov01
command -v $BULLSEYE_CMD >/dev/null 2>&1 && $BULLSEYE_CMD --off

# Clean
rm -fr $BIN_FOLDER
mkdir -p $BIN_FOLDER

## Prepare VERSION file
GIT_VERSION=`git describe --long --tags --always`
echo "git version is: $GIT_VERSION"
echo "$GIT_VERSION" > version

## Build Accelio
echo "Build Accelio....(libxio c code)"
cd $TOP_DIR
git submodule update --init
cd src/accelio/ && make distclean -si > /dev/null 2>&1;
./autogen.sh && ./configure --silent --disable-raio-build --enable-silent-rules && make -s && cp -f src/usr/.libs/libxio.so $BIN_FOLDER
if [[ $? != 0 ]] ; then
    exit 1
fi

## Build JX
echo "Build JXIO... (c code)"
cd $TOP_DIR
if [[ -n "$CODE_COVERAGE_ON" ]];then
	sudo rm -rf $COVFILE
	cov01 --on
	cov01 --status
fi
cd src/c/ && ./autogen.sh && ./configure --silent && make clean -s && make -s && cp -f src/libjxio.so $BIN_FOLDER
if [[ $? != 0 ]] ; then
    exit 1
fi
if [[ -n "$CODE_COVERAGE_ON" ]];then
	cov01 --off
fi

echo "Build JXIO... (java code)"
cd $TOP_DIR
mkdir -p docs
javadoc -classpath $LIB_FOLDER/commons-logging.jar -d $TOP_DIR/docs -sourcepath src/java/   com.mellanox.jxio
javac -cp $LIB_FOLDER/commons-logging.jar -d $BIN_FOLDER $SRC_JAVA_FILES
if [[ $? != 0 ]] ; then
    exit 1
fi

echo "Creating JXIO jar..."
cd $BIN_FOLDER && jar -cfm $TARGET ../manifest.txt com $NATIVE_LIBS
