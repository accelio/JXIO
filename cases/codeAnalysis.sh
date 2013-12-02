#!/bin/bash

##########################
###   November 2013    ###
### JXIO Code Analysis ###
##########################

# Configuring Running Directory
RUNNING_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $RUNNING_DIR

# Configure report files
JAVA_REPORT_FILE="java_analysis_report.txt"
C_REPORT_FILE="c_analysis_report.txt"

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
findbugs/findbugs-2.0.2/bin/findbugs -auxclasspath ../lib/commons-logging.jar -low  ../bin/jxio.jar > $JAVA_REPORT_FILE
echo -e "\nDone!\n"

# Cleaning up
echo -e "\nCleaning up after findbugs...!"
rm -rf findbugs
echo -e "Done!\n"

###################
# C Code Analysis #
###################

echo -e "\n--- C Code Analysis ---\n"
touch $C_REPORT_FILE
# To be completed

################
# Send Reports #
################

# Move back to running directory
cd $RUNNING_DIR

# Calculate number of errors
let "JAVA_ERRORS = `wc -l ${JAVA_REPORT_FILE} | cut -d " " -f 1`"
let "C_ERRORS = `wc -l ${C_REPORT_FILE} | cut -d " " -f 1`"
TOTAL_ERRORS=$(($JAVA_ERRORS + $C_ERRORS))

# Define report parameters
attachment="$JAVA_REPORT_FILE $C_REPORT_FILE"
subject="JXIO Code Analysis Report"
if ([ $TOTAL_ERRORS == 0 ]); then
	subject="${subject} - no issues found"
else
	subject="${subject} - found $TOTAL_ERRORS issue(s)"
fi
recipients="alongr@mellanox.com katyak@mellanox.com alexr@mellanox.com"
MAIL_MESSAGE=mail.html
MAIL_MESSAGE_HTML="<h1>JXIO Code Analysis Report</h1><br>Attached are the JXIO Java and C code analysis for `date +%d/%m/%y`.<br><br>"
if [ $JAVA_ERRORS != 0 ]; then
	MAIL_MESSAGE_HTML="${MAIL_MESSAGE_HTML} Found $JAVA_ERRORS JAVA Errors/Warnings.<br>"
fi
if [ $C_ERRORS != 0 ]; then
	MAIL_MESSAGE_HTML="${MAIL_MESSAGE_HTML} Found $C_ERRORS C Errors/Warnings.<br>"
fi

# Configure report
MAIL_MESSAGE_HTML="<html><body><font face=""Calibri"" size=3>"${MAIL_MESSAGE_HTML}"</font></body></html>"
echo $MAIL_MESSAGE_HTML > $MAIL_MESSAGE

# Send report
mutt -e "set content_type=text/html" -a $attachment -s "${subject}" -- ${recipients} < $MAIL_MESSAGE > /dev/null 2>&1
while [ $? != 0 ]; do
        echo "mutt -e "set content_type=text/html" -a $attachment -s "${subject}" -- ${recipients} < $MAIL_MESSAGE > /dev/null 2>&1"
done
echo -e  "\nSent!\n"

# Cleaning Up
rm -f $JAVA_REPORT_FILE
rm -f $C_REPORT_FILE
rm -rf $MAIL_MESSAGE
