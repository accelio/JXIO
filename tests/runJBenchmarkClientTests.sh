javac -cp "../bin/jxio.jar:../lib/commons-logging.jar" ./com/mellanox/jxio/tests/benchmarks/*.java

#java -Dlog4j.configuration=com/mellanox/jxio/tests/benchmarks/log4j.properties.jxiotest -cp "../bin/jxio.jar:../lib/commons-logging.jar:../lib/log4j-1.2.15.jar:." com.mellanox.jxio.tests.benchmarks.StatMain $1 $2 $3
java -Dlog4j.configuration=com/mellanox/jxio/tests/benchmarks/log4j.properties.jxiotest -cp "../bin/jxio.jar:../lib/commons-logging.jar:../lib/log4j-1.2.15.jar:." com.mellanox.jxio.tests.benchmarks.StatMTMain $1 $2 $3 $4
