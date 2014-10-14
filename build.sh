#!/bin/bash

# Configuring Running Directory
TOP_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $TOP_DIR
echo -e "\nThe JXIO top directory is $TOP_DIR\n"

TARGET=jxio.jar
BIN_FOLDER=$TOP_DIR/bin
LIB_FOLDER=$TOP_DIR/src/lib
SRC_JAVA_FOLDER=$TOP_DIR/src/java
SRC_JAVA_FILES="$SRC_JAVA_FOLDER/com/mellanox/jxio/*.java $SRC_JAVA_FOLDER/com/mellanox/jxio/exceptions/*.java $SRC_JAVA_FOLDER/com/mellanox/jxio/impl/*.java $SRC_JAVA_FOLDER/com/mellanox/jxio/jxioConnection/*.java $SRC_JAVA_FOLDER/com/mellanox/jxio/jxioConnection/impl/*.java $SRC_JAVA_FOLDER/org/apache/lucene/facet/taxonomy/LRUHashMap.java"
NATIVE_LIBS="libjxio.so libxio.so"

export PATH=$BULLSEYE_DIR:$PATH
# Turning off bullseye for case it was left on (only if cov01 is found on this machine)
BULLSEYE_CMD=cov01
command -v $BULLSEYE_CMD >/dev/null 2>&1 && $BULLSEYE_CMD --off

# Activate Coverity
cd $TOP_DIR
if [[ -n "$CODE_COVERAGE_ON" ]];then
	echo "Build with CODE COVERAGE ON"
	sudo rm -rf $COVFILE
	CODE_COV_ENABLE="$BULLSEYE_CMD --on && $BULLSEYE_CMD --status"
	CODE_COV_DISABLE="$BULLSEYE_CMD --off"
fi

# Clean
rm -fr $BIN_FOLDER
mkdir -p $BIN_FOLDER

## Prepare VERSION file
GIT_VERSION=`git describe --long --tags --always --dirty`
echo "git version is: $GIT_VERSION"
echo "$GIT_VERSION" > version
cp manifest.template manifest.txt
sed -i "s/Implementation-Version: .*/Implementation-Version: $GIT_VERSION/" manifest.txt

## Build Accelio
echo "Build Accelio... libxio C code"
cd $TOP_DIR
git submodule update --init
cd src/accelio/ && make distclean -si > /dev/null 2>&1;
./autogen.sh && ./configure --silent --disable-raio-build --enable-silent-rules && make -s && cp -f src/usr/.libs/libxio.so $BIN_FOLDER  && strip -s $BIN_FOLDER/libxio.so
if [[ $? != 0 ]] ; then
    echo "FAILURE! stopped JXIO build"
    exit 1
fi

## Build JXIO
echo "Build JXIO C code"
cd $TOP_DIR
cd src/c/ && ./autogen.sh && ./configure --silent && make clean -s
status=$?
$CODE_COV_ENABLE
make -s
if [[ $? != 0 ]] || [[ $status != 0 ]]; then
    echo "FAILURE! stopped JXIO build"
    exit 1
fi
cp -f src/.libs/libjxio.so $BIN_FOLDER && strip -s $BIN_FOLDER/libjxio.so

echo "Build JXIO Java code"
cd $TOP_DIR
javac -cp $LIB_FOLDER/commons-logging.jar -d $BIN_FOLDER $SRC_JAVA_FILES
if [[ $? != 0 ]] ; then
    echo "FAILURE! stopped JXIO build"
    exit 1
fi

echo "Creating JXIO Java docs"
javadoc -quiet -classpath $LIB_FOLDER/commons-logging.jar -d $TOP_DIR/docs -sourcepath src/java/ com.mellanox.jxio
if [[ $? != 0 ]] ; then
    echo "FAILURE! stopped JXIO build"
    exit 1
fi

echo "Creating JXIO jar..."
cd $BIN_FOLDER && jar -cfm $TARGET ../manifest.txt com org $NATIVE_LIBS
if [[ $? != 0 ]] ; then
    echo "FAILURE! stopped JXIO build"
    exit 1
fi
