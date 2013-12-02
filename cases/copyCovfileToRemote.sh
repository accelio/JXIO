#!/bin/bash

sessionId=$1

if [[ -z "$CODE_COVERAGE_ON" ]];then
	exit 0
fi

echoPrefix=`basename $0`

# managing Bullseye - C coverage

cCoverageResultDir=$COVFILES_REPO_DIR/$sessionId/$C_REPO_DIR_NAME/$RESULTS_DIRNAME
echo "$echoPrefix: cp $COVFILE $cCoverageResultDir/${C_COVFILE_PREFIX}_`hostname`${C_COVFILE_SUFFIX}"
cp $COVFILE $cCoverageResultDir/${C_COVFILE_PREFIX}_`hostname`${C_COVFILE_SUFFIX}

# managing Cobertura - Java coverage

javaCoverageResultDir=$COVFILES_REPO_DIR/$sessionId/$JAVA_REPO_DIR_NAME/$RESULTS_DIRNAME
echo "$echoPrefix: cp $COBERTURA_COVFILE $javaCoverageResultDir/${JAVA_COVFILE_PREFIX}_`hostname`${JAVA_COVFILE_SUFFIX}"
cp $COBERTURA_COVFILE $javaCoverageResultDir/${JAVA_COVFILE_PREFIX}_`hostname`${JAVA_COVFILE_SUFFIX}


 


