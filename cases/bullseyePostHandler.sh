#!/bin/bash

if [[ -z "$COVFILE" ]];then
	exit 0
fi

echoPrefix=`basename $0`

#getting the covfiles from the machines



#echo "$machine:$covfileInputDir/${COVFILE_PREFIX}${machine}${COVFILE_SUFFIX} $covfileDestDir"
#echo "$echoPrefix: pdsh -w $machinesHostnames scp $covfileInputDir/${COVFILE_PREFIX}%h${COVFILE_SUFFIX} $covfileDestDir"
#pdsh -w $machinesHostnames scp $covfileInputDir/${COVFILE_PREFIX}%h${COVFILE_SUFFIX} $covfileDestDir
#echo "$echoPrefix: pdsh -w $machinesHostnames scp $COVFILE $covfileDestDir/${COVFILE_PREFIX}%h${COVFILE_SUFFIX}"
#pdsh -w $machinesHostnames scp $COVFILE $covfileDestDir/${COVFILE_PREFIX}%h${COVFILE_SUFFIX}

#merging the covfiles

mergedCovfileTemp=$JXIO_DIR/${MERGED_COVFILE_PREFIX}${COVFILE_SUFFIX}
echo "$echoPrefix: merge command is: covmerge --create --file $mergedCovfileTemp $COVERAGE_RESULTS_DIR/*${COVFILE_SUFFIX}"
covmerge --create --file $mergedCovfileTemp $COVERAGE_RESULTS_DIR/*${COVFILE_SUFFIX}

codeCoverageSummaryTemp=$JXIO_DIR/$SUMMARY_FILENAME
codeCoverageSummaryFinal=$COVERAGE_DIR/$SUMMARY_FILENAME
covdir --file $mergedCovfileTemp >> $codeCoverageSummaryTemp

mv $mergedCovfileTemp $COVERAGE_DIR
mv $codeCoverageSummaryTemp $COVERAGE_DIR

# commiting the results

echo "$echoPrefix: covselect --file $mergedCovfileTemp -i $COVERAGE_EXCLUDES"
covselect --file $mergedCovfileTemp -i $COVERAGE_EXCLUDES
echo "$echoPrefix: eval $CODE_COVERAGE_COMMIT_SCRIPT_PATH --branch $GIT_BRANCH --product $PRODUCT_NAME --team $TEAM_NAME --version $VERSION_SHORT_FORMAT --path $COVERAGE_RESULTS_DIR"
if [[ -z $BULLSEYE_DRYRUN ]];then
	eval $CODE_COVERAGE_COMMIT_SCRIPT_PATH --branch $GIT_BRANCH --product $PRODUCT_NAME --team $TEAM_NAME --version $VERSION_SHORT_FORMAT --path $COVERAGE_RESULTS_DIR
else
	echo "$echoPrefix: dry-run mode - the coverage-report won't be sent"
fi



