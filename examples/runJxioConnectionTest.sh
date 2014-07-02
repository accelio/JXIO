#1=type(server/client) 2=server ip, 3=port, 4=server num workers, 5=bytes to transfer, 6= number of input clients, 7=number of output clients

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
javac -cp "../bin/jxio.jar:../src/lib/commons-logging.jar:." com/mellanox/jxio/tests/benchmarks/jxioConnection/*.java
if [[ $? != 0 ]] ; then
    exit 1
fi

java_coverage_props=""
if [[ -n "$CODE_COVERAGE_ON" ]];then
        java_coverage_props="-D"$COBERTURA_COVFILE_PATH_PROP_NAME"="$COBERTURA_COVFILE
fi

echo "running InputStream Benchmark"
sudo killall java
sleep 1
if [ $1 == "s" ]; then
java -Dlog4j.configuration=com/mellanox/jxio/tests/log4j.properties.jxiotest -cp "$COBERTURA_JAR_PATH:../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." $java_coverage_props com.mellanox.jxio.tests.benchmarks.jxioConnection.StreamServer $2 $3 $4 | tee server.txt &
sleep 10
sudo killall java

elif  [ $1 == "c" ]; then
java -Dlog4j.configuration=com/mellanox/jxio/tests/log4j.properties.jxiotest -cp "$COBERTURA_JAR_PATH:../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." $java_coverage_props com.mellanox.jxio.tests.benchmarks.jxioConnection.StreamClient $2 $3 $5 $6 $7 | tee res.txt

fi