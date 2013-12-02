#!/bin/bash

if [[ -z "$CODE_COVERAGE_ON" ]];then
	exit 0
fi

echoPrefix=`basename $0`

if [[ -e "$COBERTURA_COVFILE" ]];then
	rm -rf $COBERTURA_COVFILE
fi
bash $COBERTURA_DIR/cobertura-instrument.sh --datafile $COBERTURA_COVFILE $JXIO_DIR/$JXIO_RELATIVE_JAR_PATH
if (( $? != 0 ));then
	echo "$echoPrefix: cobertura instrumentation failed!"
	exit 1
fi
