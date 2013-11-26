#!/bin/bash

sessionId=$1
codeCoverageOn=$2

if [[ -n "$codeCoverageOn" ]];then
	echoPrefix=`basename $0`
	coverageResultDir=$COVFILES_REPO_DIR/$sessionId/$RESULTS_DIRNAME
	echo "$echoPrefix: cp $COVFILE $coverageResultDir/${COVFILE_PREFIX}_`hostname`${COVFILE_SUFFIX}"
	cp $COVFILE $coverageResultDir/${COVFILE_PREFIX}_`hostname`${COVFILE_SUFFIX}
fi





 


