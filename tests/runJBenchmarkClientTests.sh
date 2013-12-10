javac -cp "../bin/jxio.jar:../src/lib/commons-logging.jar" ./com/mellanox/jxio/tests/benchmarks/*.java

#The below client is for Connection Establishment perfromance  test
#$1 is Server IP $2 Server port $3 Num sessions 
#java -Dlog4j.configuration=com/mellanox/jxio/tests/benchmarks/log4j.properties.jxiotest -cp "../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." com.mellanox.jxio.tests.benchmarks.StatMain $1 $2 $3

#The Below client is for MultyThreaded Connection Establishment performance test
#$1 is Server IP $2 Server port $3 Num sessions $4 Num Threads (Pay attention that num sessiosn should be multiplier of num threads
java -Dlog4j.configuration=com/mellanox/jxio/tests/benchmarks/log4j.properties.jxiotest -cp "../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." com.mellanox.jxio.tests.benchmarks.StatMTMain $1 $2 $3 $4

#The Below client is for DataPath perfromance test (TPS & BW)
#$1 is Server IP $2 Server port  $3 Msg size  
java -Dlog4j.configuration=com/mellanox/jxio/tests/benchmarks/log4j.properties.jxiotest -cp "../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." com.mellanox.jxio.tests.benchmarks.DataPathTestClient $1 $2 $3
