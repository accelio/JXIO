#!/bin/bash

# Configuring Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR
echo -e "\nThe JXIO Test directory is $DIR\n" 

# Configuring Parameters
TARGET=jx.jar
JX_SO=libjx.so
FILES=java/src/com/mellanox/*.java

# Clean
rm -f tests/jx.jar
rm -f java/bin/libjx.so
rm -f tests/libjx.so
rm -f java/bin/libxio.so
rm -f tests/libxio.so

## Build
#git submodule init
#git submodule update
cd libxio/;
#git pull origin master
./autogen.sh && ./configure && make ; cd ..
cd c/; ./autogen.sh && ./configure && make ; cd ..
mkdir -p java/bin
javac -d java/bin/ $FILES
cd java/bin ;jar -cvf $TARGET com ; cd ../..
cp -f java/bin/$TARGET tests/
cp -f c/src/libjx.so java/bin
cp -f c/src/libjx.so tests/
cp -f libxio/src/usr/libxio.so java/bin
cp -f libxio/src/usr/libxio.so tests/



