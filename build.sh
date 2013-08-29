#!/bin/bash
TARGET=jx.jar
JX_SO=libjx.so

FILES=java/src/com/mellanox/*.java

git submodule init
git submodule update
cd libxio/; ./autogen.sh && ./configure && make ; cd ..
cd c/; ./autogen.sh && ./configure && make ; cd ..
mkdir -p java/bin
javac -d java/bin/ $FILES
cd java/bin ;jar -cvf $TARGET com ; cd ../..
cp java/bin/$TARGET tests/
cp c/src/libjx.so java/bin
cp c/src/libjx.so tests/
cp  libxio/src/usr/libxio.so java/bin
cp  libxio/src/usr/libxio.so tests/



