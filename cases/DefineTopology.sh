#!/bin/bash

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TOP_DIR=$DIR/..
cd $DIR

# Arguments Check
if [ $# -ne 3 ]; then
	echo "[$0] Missing Parameters!"
	echo "Usage: $0 [Probability XML Directory] [Probability XML Filename] [Path to Topology XML file]"
	exit 1
fi

# Parameters Configuration
XMLDir=$1
XMLFile=$2
TopoFile=$3
XMLPath="${XMLDir}/${XMLFile}"

# Parameters Check
if [ ! -f ${XMLPath} ]; then
	echo "[$0] XML file not found!"
	exit 1
fi
if [ ! -f ${TopoFile} ]; then
        echo "[$0] Topology XML file not found!"
        exit 1
fi

# Find The Machines Tag
start=0
end=0
let i=1
while read line ; do
	if [[ $line == *\<machines\>* ]]; then
		start=$i;
	elif [[ $line == *\</machines\>* ]]; then
		end=$i;
	fi
	i=$((i+1));
done <"$XMLPath"

# Remove the Machines Tag
if [ ${start} -ne 0 ] || [ ${end} -ne 0 ]; then
	sed -i "${start},${end}d" $XMLPath
fi

# Insert New Machine Topology
echo "<root>" > ${XMLDir}/new_probability.xml
cat ${TopoFile} >> ${XMLDir}/new_probability.xml
cat ${XMLPath} | grep -v "<root>" >> ${XMLDir}/new_probability.xml
mv -f ${XMLDir}/new_probability.xml ${XMLDir}/${XMLFile}

echo "DONE! Machines in ${XMLDir}/${XMLFile} are now defined!"
