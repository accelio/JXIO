#!/bin/bash

cd $JXIO_DIR
#TOP_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#cd $TOP_DIR/..
bash $JXIO_DIR/build.sh
cp -r $JXIO_DIR $JXIO_REMOTE_DIR



