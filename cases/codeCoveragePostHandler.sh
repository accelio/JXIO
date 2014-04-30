#!/bin/bash

sessionId=$1
	
if [[ -z "$CODE_COVERAGE_ON" ]];then
	exit 0
fi

function getFiled()
{
	grep "$1" "$2" | awk -v fieldNum="$3" '{print $fieldNum}'
}

function convertLinuxPathToWindows()
{
	echo $1 | awk '{gsub("/.autodirect","file://mtrlabfs01",$0); print $0}'
}

function getAverage()
{
	local sum=0
	local count=0
	for num in $@;do
		numWithoutPercentageMark=`echo $num | awk 'BEGIN{FS="%"}{print $1}'`
		sum=`echo "scale=1; ${sum}+${numWithoutPercentageMark}" | bc`
		count=$((count+1))
	done
	local total=`echo "scale=0; ${sum}/${count}" | bc`
	echo "${total}%"
}

echoPrefix=`basename $0`
coverageDir=$COVFILES_REPO_DIR/$sessionId

# managing Bullseye - C coverage

cCoverageDir=$coverageDir/$C_REPO_DIR_NAME
cCoverageResultDir=$cCoverageDir/$RESULTS_DIRNAME

cMergedCovfileTemp=$JXIO_DIR/${MERGED_COVFILE_PREFIX}${C_COVFILE_SUFFIX}
echo "$echoPrefix: merge command is: $BULLSEYE_DIR/covmerge --create --file $cMergedCovfileTemp $cCoverageResultDir/*${C_COVFILE_SUFFIX}"
$BULLSEYE_DIR/covmerge --create --file $cMergedCovfileTemp $cCoverageResultDir/*${C_COVFILE_SUFFIX}

cCoverageSummaryTemp=$JXIO_DIR/$SUMMARY_FILENAME
$BULLSEYE_DIR/covdir --file $cMergedCovfileTemp >> $cCoverageSummaryTemp

cFunctionsCoverage=`getFiled "Total" $cCoverageSummaryTemp 6`
cBlocksCoverage=`getFiled "Total" $cCoverageSummaryTemp 11`

# commiting the results
echo "$echoPrefix: $BULLSEYE_DIR/covselect --file $cMergedCovfileTemp --import $BULLSEYE_COVERAGE_EXCLUDES --remove"
$BULLSEYE_DIR/covselect --file $cMergedCovfileTemp --import $BULLSEYE_COVERAGE_EXCLUDES --remove
echo "$echoPrefix: eval $BULLSEYE_COVERAGE_COMMIT_SCRIPT_PATH --branch $GIT_BRANCH --product $PRODUCT_NAME --team $TEAM_NAME --version $VERSION_SHORT_FORMAT --path $cCoverageResultDir"
if [[ -z $CODE_COVERAGE_DRYRUN ]];then
	eval $BULLSEYE_COVERAGE_COMMIT_SCRIPT_PATH --branch $GIT_BRANCH --product $PRODUCT_NAME --team $TEAM_NAME --version $VERSION_SHORT_FORMAT --path $cCoverageResultDir
else
	echo "$echoPrefix: dry-run mode - the coverage-report won't be sent"
fi

cCoverageReportDir=$cCoverageDir/$REPORT_DIRNAME
$BULLSEYE_DIR/covhtml --file $cMergedCovfileTemp $cCoverageReportDir
sudo chmod -R 755 $cCoverageReportDir
cReportDir="$cCoverageReportDir/index.html"
cReportDirWindows=`convertLinuxPathToWindows $cReportDir`

mv $cMergedCovfileTemp $cCoverageDir
mv $cCoverageSummaryTemp $cCoverageDir

# managing Cobertura - Java coverage

javaCoverageDir=$coverageDir/$JAVA_REPO_DIR_NAME
javaCoverageResultDir=$javaCoverageDir/$RESULTS_DIRNAME
javaCoverageFinal=$javaCoverageDir/${MERGED_COVFILE_PREFIX}${JAVA_COVFILE_SUFFIX}

javaCovfiles="`echo $javaCoverageResultDir/*`"
bash $COBERTURA_DIR/cobertura-merge.sh --datafile $javaCoverageFinal $javaCovfiles
if (( $? != 0 ));then
	echo "$echoPrefix: cobertura-merge failure"
	exit 1
fi

javaCoverageSummary=$javaCoverageDir/$SUMMARY_FILENAME
javaSrcRemote=$javaCoverageDir/$SRC_DIRNAME
javaSrcLocal=$JXIO_DIR/$JXIO_RELATIVE_JAVA_SRC
if [[ $javaSrcLocal == "/" ]];then
	echo "$echoPrefix: java source can not be found"
	exit 1
fi

# copy sources to remote for the report to use
mkdir $javaSrcRemote
cp -r $javaSrcLocal/* $javaSrcRemote


tmpFile=$javaCoverageDir/DELETE_ME_mars
bash $COBERTURA_DIR/cobertura-check.sh --datafile $javaCoverageFinal --branch 100 --line 100 --totalbranch 100 --totalline 100 $javaSrcRemote 2&> $tmpFile
#if (( $? != 30 ));then
#	echo "$echoPrefix: cobertura-check failure"	
#	exit 1
#fi
cat $tmpFile | awk '($1=="Project"){print $5 ": " $9}' > $javaCoverageSummary
rm $tmpFile

javaCoverageReportDir=$javaCoverageDir/$REPORT_DIRNAME
bash $COBERTURA_DIR/cobertura-report.sh --datafile $javaCoverageFinal --destination $javaCoverageReportDir --srcdir $javaSrcRemote
if (( $? != 0 ));then
	echo "$echoPrefix: cobertura-report failure"
	exit 1
fi
sudo chmod -R 755 $javaCoverageReportDir
javaReportDir="$javaCoverageReportDir/index.html"
javaReportDirWindows=`convertLinuxPathToWindows $javaReportDir`

javaLinesCoverage=`getFiled "line" $javaCoverageSummary 2`
javaBlocksCoverage=`getFiled "branch" $javaCoverageSummary 2`

# Managing the report

totalCoverage=`getAverage $cFunctionsCoverage $cBlocksCoverage $javaLinesCoverage $javaBlocksCoverage`

attachment=""
subjectCation="JXIO Code Coverage Report"
subject="$subjectCation - total $totalCoverage"
recipients="alongr@mellanox.com katyak@mellanox.com alexr@mellanox.com dinal@mellanox.com"

mailMessageFile=$coverageDir/report.html
message="<h1>${subjectCation}</h1><br>
	<h3>JXIO Java and C code coverage results for `date +%d/%m/%y`:</h3><br><br>
	<b>Java: </b> Lines: ${javaLinesCoverage}. Blocks: ${javaBlocksCoverage} <br>
	<b>Full Java report: </b><a href=$javaReportDirWindows>Windows</a>  <a href=$javaReportDir>Linux</a> <br><br>
	<b>C: </b> Functions: ${cFunctionsCoverage}. Blocks: ${cBlocksCoverage} <br>
	<b>Full C report: </b><a href=$cReportDirWindows>Windows</a>  <a href=$cReportDir>Linux</a> <br>
	<h3>Total: $totalCoverage</h3>"
	
message="<html><body><font face=""Calibri"" size=3>"${message}"</font></body></html>"
echo $message > $mailMessageFile

# Send report
mutt -e "set content_type=text/html" -s "${subject}" -- ${recipients} < $mailMessageFile > /dev/null 2>&1
while [ $? != 0 ]; do
    mutt -e "set content_type=text/html" -s "${subject}" -- ${recipients} < $mailMessageFile > /dev/null 2>&1
done
echo -e  "\nSent!\n"
