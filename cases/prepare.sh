#!/bin/bash

sessionId=$1
codeCoverageOn=$2

if [[ -n "$codeCoverageOn" ]];then
	coverageResultDir=$COVFILES_REPO_DIR/$sessionId/$RESULTS_DIRNAME
	mkdir -p $coverageResultDir
fi

