#!/bin/bash

##########################
###   November 2013    ###
### JXIO Code Analysis ###
##########################

# Configuring Running Directory
RUNNING_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $RUNNING_DIR

# Configure report files
REPORTS_STORAGE="/.autodirect/acclgwork/jxio/static_code_analysis/"
JAVA_REPORT_FILE="java_analysis_report.txt"
C_REPORT_FILE="c_analysis_report.html"

# Configure analysis filters
JAVA_FILTER="java_analysis_filter.xml"

######################
# Java Code Analysis #
######################

echo -e "\n==== JXIO Code Analysis ===\n"

echo -e "\n--- Java Code Analysis ---\n"

# Create a temporary folder
mkdir findbugs
cd findbugs

# Fetch latest findbugs (with no updates) version
echo -e "\nFetching findbugs...!\n"
wget http://prdownloads.sourceforge.net/findbugs/findbugs-noUpdateChecks-2.0.2.tar.gz?download
echo -e "Done!\n"

# Untar
echo -e "\nUntaring findbugs...!"
tar -xzf findbugs-noUpdateChecks-2.0.2.tar.gz
echo -e "Done!\n"

# Move back to running directory
cd $RUNNING_DIR

# Run findbugs
echo -e "\nRunning findbugs...!\n"
findbugs/findbugs-2.0.2/bin/findbugs -exclude $JAVA_FILTER -auxclasspath ../src/lib/commons-logging.jar -low  ../bin/jxio.jar > $JAVA_REPORT_FILE
if [ $? != 0 ]; then
	echo -e "\n[ERROR] Exeption occurred while running findbugs!"
	echo -e "It is possible that the .jar file needed doesn't exist."
	echo -e "Try rebuilding JXIO by running ./build.sh.\n"
	exit 1
fi
echo -e "\nDone!\n"

# Calculate number of errors
let "JAVA_ERRORS = `wc -l ${JAVA_REPORT_FILE} | cut -d " " -f 1`"

# Cleaning up
echo -e "\nCleaning up after findbugs...!"
rm -rf findbugs
echo -e "Done!\n"

###################
# C Code Analysis #
###################

echo -e "\n--- C Code Analysis ---\n"

# Move back to running directory
cd $RUNNING_DIR

# Move to the C code src folder
cd ../src/c/src/

# Run Covertiy
echo -e "\nRunning Coverity...!\n"
make cov > $RUNNING_DIR/$C_REPORT_FILE
if [ $? != 0 ]; then
	echo -e "\n[ERROR] Error while running coverity!"
	exit 1
fi
echo -e "\nDone!\n"

# Calculate number of errors
let "C_ERRORS = `grep "C/C++ error" ${RUNNING_DIR}/${C_REPORT_FILE} | head -n 1 | cut -d " " -f 2`"

# Config Report
cp cov-build/c/output/errors/index.html ${RUNNING_DIR}/${C_REPORT_FILE}

#################
# Store Reports #
#################

echo -e "\n--- Storing Reports ---\n"

# Move back to running directory
cd $RUNNING_DIR

# Storing
folder=`date +"%Y_%m_%d"`
mkdir -p ${REPORTS_STORAGE}/${folder}

# Store Java report
cp ${JAVA_REPORT_FILE} ${REPORTS_STORAGE}/${folder}

# Store C report
toreplace="1\/"
replacewith="\\\\\\\\mtrlabfs01\\\\acclgwork\\\\jxio\\\\static_code_analysis\\\\$folder\\\\src\\\\cov-build\\\\c\\\\output\\\\errors\\\\1\\\\"
sed -i "s/$toreplace/$replacewith/g" ${C_REPORT_FILE}

cp ${C_REPORT_FILE} ${REPORTS_STORAGE}/${folder}
cp -r ../src/c/src/ ${REPORTS_STORAGE}/${folder}

################
# Send Reports #
################

echo -e "\n--- Sending Report ---\n"

# Move back to running directory
cd $RUNNING_DIR

# Calculate number of errors
TOTAL_ERRORS=$(($JAVA_ERRORS + $C_ERRORS))

# Define report parameters
attachment=""
subject="JXIO Code Analysis Report"
if ([ $TOTAL_ERRORS == 0 ]); then
	subject="${subject} - no issues found"
else
	subject="${subject} - found $TOTAL_ERRORS issue(s)"
fi
recipients="alongr@mellanox.com katyak@mellanox.com alexr@mellanox.com yiftahs@mellanox.com"
MAIL_MESSAGE=mail.html
MAIL_MESSAGE_HTML="<h1>JXIO Code Analysis Report</h1><br>Attached are the JXIO Java and C code analysis for `date +%d/%m/%y`.<br><br>"
if [ $JAVA_ERRORS != 0 ]; then
	MAIL_MESSAGE_HTML="${MAIL_MESSAGE_HTML} Found $JAVA_ERRORS JAVA Errors/Warnings.<br>"
	attachment="$attachment $JAVA_REPORT_FILE"
else
	MAIL_MESSAGE_HTML="${MAIL_MESSAGE_HTML} No JAVA Errors/Warnings Found.<br>"
fi
if [ $C_ERRORS != 0 ]; then
	MAIL_MESSAGE_HTML="${MAIL_MESSAGE_HTML} Found $C_ERRORS C Errors/Warnings.<br>"
	attachment="$attachment $C_REPORT_FILE"
else
	MAIL_MESSAGE_HTML="${MAIL_MESSAGE_HTML} No C Errors/Warnings Found.<br>"
fi

if [ $JAVA_ERRORS != 0 ] || [ $C_ERRORS != 0 ]; then
	attachment="-a $attachment"
fi

# Configure report
MAIL_MESSAGE_HTML="<html><body><font face=""Calibri"" size=3>"${MAIL_MESSAGE_HTML}"</font></body></html>"
echo $MAIL_MESSAGE_HTML > $MAIL_MESSAGE

# Send report
mutt -e "set content_type=text/html" $attachment -s "${subject}" -- ${recipients} < $MAIL_MESSAGE > /dev/null 2>&1
while [ $? != 0 ]; do
        mutt -e "set content_type=text/html" $attachment -s "${subject}" -- ${recipients} < $MAIL_MESSAGE > /dev/null 2>&1
done
echo -e  "\nSent!\n"

# Cleaning Up
rm -f $JAVA_REPORT_FILE
rm -f $C_REPORT_FILE
rm -rf $MAIL_MESSAGE
