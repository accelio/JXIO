#!/bin/bash

jxioDir_tmp=$1
sessionId_tmp=$2
bullseye_tmp=$3

export JXIO_DIR="$jxioDir_tmp"
export JXIO_REMOTE_DIR="/.autodirect/mtrswgwork/UDA/jxio"
export PRODUCT_NAME="jxio"
export BULLSEYE_DIR="/.autodirect/mtrswgwork/UDA/bullseye/bin"
export PATH="$BULLSEYE_DIR:$PATH"

if [[ -n "$bullseye_tmp" ]]
then
	export MERGED_COVFILE_PREFIX="merged_covfile"
	export RESULTS_DIRNAME="results"
	export SUMMARY_FILENAME="cov_summary.txt"
	export COVFILE_PREFIX="covfile"
	export COVFILE_SUFFIX=".cov"
	export COVFILE="$JXIO_DIR/${COVFILE_PREFIX}${COVFILE_SUFFIX}"
	export COVFILES_REPO_DIR="$JXIO_REMOTE_DIR/covfiles"
	export COVERAGE_EXCLUDES="$JXIO_REMOTE_DIR/buillseye_excludes"
	export COVERAGE_DIR="$COVFILES_REPO_DIR/$sessionId_tmp"
	export COVERAGE_RESULTS_DIR="$COVERAGE_DIR/$RESULTS_DIRNAME"
	export CODE_COVERAGE_COMMIT_SCRIPT_PATH="/.autodirect/mswg/utils/bin/coverage/commit_cov_files.sh"
	export GIT_BRANCH="master"
	export TEAM_NAME="uda"
	export VERSION_SHORT_FORMAT="1.0"
	export BULLSEYE_DRYRUN=""

	mkdir -p $COVERAGE_RESULTS_DIR
else
	unset COVFILE
fi
