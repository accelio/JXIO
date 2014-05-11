#!/bin/sh


# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TOP_DIR="$DIR/../"
cd $DIR

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
        echo -e "\nError: JAVA_HOME not defined in shell!"
        exit 1
fi

# Compile
echo -e "\nCompiling JAVA files...."
javac -cp "$TOP_DIR/bin/jxio.jar:$TOP_DIR/src/lib/commons-logging.jar" ./com/mellanox/jxio/tests/benchmarks/InputStreamServer.java ./com/mellanox/jxio/tests/benchmarks/InputStreamClient.java ./com/mellanox/jxio/tests/benchmarks/ServerReader.java
if [[ $? != 0 ]] ; then
    exit 1
fi

java_coverage_props=""
if [[ -n "$CODE_COVERAGE_ON" ]];then
        java_coverage_props="-D"$COBERTURA_COVFILE_PATH_PROP_NAME"="$COBERTURA_COVFILE
fi


#echo bw[Mb/s] >> resbenchmark.csv
#pdsh -w r-sw-fatty[12] "sudo killall java"

echo "running InputStream Benchmark"
sudo killall java
if [ $1 == "s" ]; then
cd $TOP_DIR/tests && java -Dlog4j.configuration=com/mellanox/jxio/tests/log4j.properties.jxiotest -cp "$COBERTURA_JAR_PATH:$TOP_DIR/bin/jxio.jar:$TOP_DIR/src/lib/commons-logging.jar:$TOP_DIR/src/lib/log4j-1.2.15.jar:." $java_coverage_props com.mellanox.jxio.tests.benchmarks.InputStreamServer $2 $3 $4 $5 | tee server.txt &

elif  [ $1 == "c" ]; then
for ((j=1;j<=$9;j=j+1)); do
#pdsh -w r-sw-fatty[11] "sudo killall java"
sleep 2
cd $TOP_DIR/tests && java -Dlog4j.configuration=com/mellanox/jxio/tests/log4j.properties.jxiotest -cp "$COBERTURA_JAR_PATH:$TOP_DIR/bin/jxio.jar:$TOP_DIR/src/lib/commons-logging.jar:$TOP_DIR/src/lib/log4j-1.2.15.jar:." $java_coverage_props com.mellanox.jxio.tests.benchmarks.InputStreamClient rdma://$2:$3/data?size=$6 $7 $8 | tee res.txt
#res=`grep "BW" res.txt | awk '{ print $8 }'`
#echo $res >> resbenchmark.csv
done

fi
