#!/bin/bash

sessionId=$1
codeCoverageOn=$2

if [[ -n "$codeCoverageOn" ]];then
	cCoverageResultDir=$COVFILES_REPO_DIR/$sessionId/$C_REPO_DIR_NAME/$RESULTS_DIRNAME
	javaCoverageResultDir=$COVFILES_REPO_DIR/$sessionId/$JAVA_REPO_DIR_NAME/$RESULTS_DIRNAME
	mkdir -p $cCoverageResultDir $javaCoverageResultDir
fi

