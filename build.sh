#!/bin/bash

# Configuring Running Directory
TOP_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $TOP_DIR
echo -e "\nThe JXIO top directory is $TOP_DIR\n"

TARGET=jxio.jar
BIN_FOLDER=$TOP_DIR/bin
SRC_JAVA_FOLDER=$TOP_DIR/java/src
SRC_JAVA_FILES="$SRC_JAVA_FOLDER/com/mellanox/jxio/*.java $SRC_JAVA_FOLDER/com/mellanox/jxio/impl/*.java"
NATIVE_LIBS="libjxio.so libxio.so"

# Clean
rm -fr $BIN_FOLDER
mkdir -p $BIN_FOLDER

## Build Accelio
echo "Build Accelio....(libxio c code)"
cd $TOP_DIR
git submodule update --init
cd accelio/ && ./autogen.sh && ./configure --disable-raio-build && make && cp -f src/usr/.libs/libxio.so $BIN_FOLDER
if [[ $? != 0 ]] ; then
    exit 1
fi

## Build JX
echo "Build JXIO... (c code)"
cd $TOP_DIR
cd c/ && ./autogen.sh && ./configure && make && cp -f src/libjxio.so $BIN_FOLDER
if [[ $? != 0 ]] ; then
    exit 1
fi

echo "Build JXIO... (java code)"
cd $TOP_DIR
javac -cp lib/commons-logging.jar -d $BIN_FOLDER $SRC_JAVA_FILES

echo "Creating JXIO jar..."
cd $BIN_FOLDER; jar -cf $TARGET com $NATIVE_LIBS
