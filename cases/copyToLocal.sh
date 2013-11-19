#!/bin/bash

echoPrefix=`basename $0`
echo "$echoPrefix: cp -r $JXIO_REMOTE_DIR/$PRODUCT_NAME/* $JXIO_DIR"
cp -r $JXIO_REMOTE_DIR/$PRODUCT_NAME/* $JXIO_DIR
 


