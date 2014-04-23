#!/bin/bash

sessionId=$1
echo "The session ID is $sessionId"
codeCoverageOn=$2
echo "codeCoverageOn is $codeCoverageOn"

if [[ -n "$codeCoverageOn" ]];then
	cCoverageResultDir=$COVFILES_REPO_DIR/$sessionId/$C_REPO_DIR_NAME/$RESULTS_DIRNAME
	echo "cCoverageResultDir is $cCoverageResultDir"
	javaCoverageResultDir=$COVFILES_REPO_DIR/$sessionId/$JAVA_REPO_DIR_NAME/$RESULTS_DIRNAME
	echo "javaCoverageResultDir is $javaCoverageResultDir"
	mkdir -p $cCoverageResultDir $javaCoverageResultDir
fi
