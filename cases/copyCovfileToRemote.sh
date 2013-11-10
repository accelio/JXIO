#!/bin/bash

if [[ -n "$COVFILE" ]];then
	cp $COVFILE $COVERAGE_RESULTS_DIR/${COVFILE_PREFIX}_`hostname`${COVFILE_SUFFIX}
fi





 


