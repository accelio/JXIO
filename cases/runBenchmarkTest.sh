# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TOP_DIR=$DIR/..
cd $DIR

# Define Parameters
if [ ! -z ${10} ]; then
	r_option="-r ${10}"
fi
ip=`echo $3 | sed 's/172.30.21/36.0.0/g'`

# Run the benchmarks
let "run_stat=1"
echo -e "\nRunning benchmarks...."
bash $TOP_DIR/cases/timeout.sh $1 &
python ${TOP_DIR}/tests/runJBenchmarkTest.py $2 -a ${ip} -p $4 -t $5 -i $6 -o $7 -m $8 -u $9 ${r_option}

# Kill processes on server
echo -e "\nRunning JXIO processes on server...".
ssh root@$3 'bash -s' < ${TOP_DIR}/cases/killall.sh "benchmark"





