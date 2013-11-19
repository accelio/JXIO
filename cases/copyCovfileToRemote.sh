#!/bin/bash

sessionId=$1
codeCoverageOn=$2

if [[ -n "$codeCoverageOn" ]];then
	coverageResultDir=$COVFILES_REPO_DIR/$session_id/$RESULTS_DIRNAME
	cp $COVFILE $coverageResultDir/${COVFILE_PREFIX}_`hostname`${COVFILE_SUFFIX}
fi





 


