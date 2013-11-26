#!/bin/bash

echoPrefix=`basename $0`
projectRemoteDir=$JXIO_REMOTE_DIR/$PRODUCT_NAME
cd $JXIO_DIR
bash $JXIO_DIR/build.sh $1
echo "$echoPrefix: sudo rm -rf $projectRemoteDir"
sudo rm -rf $projectRemoteDir
echo "$echoPrefix: cp -r $JXIO_DIR $projectRemoteDir"
cp -r $JXIO_DIR $projectRemoteDir
echo "$echoPrefix: Done"
