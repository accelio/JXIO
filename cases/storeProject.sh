#!/bin/bash

echoPrefix=`basename $0`

projectRemoteDir=$JXIO_REMOTE_DIR/$PRODUCT_NAME
echo "$echoPrefix: sudo rm -rf $projectRemoteDir"
sudo rm -rf $projectRemoteDir
echo "$echoPrefix: cp -r $JXIO_DIR $projectRemoteDir"
cp -r $JXIO_DIR $projectRemoteDir
