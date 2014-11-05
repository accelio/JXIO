#!/usr/bin/env python

import os
import sys
import getopt
import subprocess

try:
	f=os.popen('which python')
	pythonpath=f.readline()
	print "PYTHONPATH is %s" % pythonpath
except:
	print "\nPYTHON PATH not found!"
	sys.exit(1)

sys.path.append(pythonpath)

# Checks to see if JAVA path is valid
cmd = "[ ! -e ${JAVA_HOME} ]"
if(os.system(cmd) == 0):
	print "ERROR: JAVA not found!"
	sys.exit(1)

# Get Running Directory
dir =  os.path.dirname(os.path.realpath(__file__))
os.chdir(dir);

# Exporting LD library
cmd = "export LD_LIBRARY_PATH=%s" % (dir)
os.system(cmd)
print "LD library is: %s" % dir


def usage():
    print "Usage: ./tests/RunJBenchmarkTest.py OPTION"
    print "\t-h  | --help			print help"
    print "\t-c  | --client			run a client test"
    print "\t-s  | --server			run a server test"
    print "\t-a  | --address			Server's address"
    print "\t-p  | --port			Server's port (default is 2222)"
    print "\t-t  | --thread			number of threads (default is 1)"
    print "\t-i  | --in			in message size in Bytes (default is 64/0 for server/client)"  
    print "\t-o  | --out			out message size In Bytes(default is 0/64 for server/client)"   
    print "\t-b  | --burst			initial client burst size (only default is 50). Must be the same for client and server"
    print "\t-u  | --cpu			core number or range of cores to run the threads on (default is 1)"  
    print "\t-f  | --file			path to results file (client only, default is no file for writing)"  
    print "\t-r  | --runs			number of iterations of the test (client only, default is 50). Single iteration=measure of every 40000/MsgSizeInK"
    print "examples :"
    print "\ttests/runJBenchmarkTest.py -c -a 1.1.1.1 -p 2222 -t 2 -i 0 -o 64 -b 100 -u 12 -f /tmp/results.csv -r 10"  
    print "\ttests/runJBenchmarkTest.py -s -a 1.1.1.1 -p 2222 -t 2 -i 64 -o 0 -b 100 -u 12"   


options, remainder = getopt.gnu_getopt(sys.argv[1:], '?hcsa:p:t:i:o:b:u:f:r:', ['help',
                                                                'address=',
                                                                'port=',
                                                                'thread=',
                                                                'in='
                                                                'out=',
                                                                'burst=',
                                                                'cpu=',
                                                                'file=',
                                                                'runs='])
                                                                
test_type = None
address = None

port = 2222
thread = 1
burst = 50
core = 1
file = "no_file"
runs = 50

in1 = None
out1 = None

                                                               
for opt, arg in options:
        if opt in ('-?', '-h', '--help'):
            usage()
            sys.exit(0)
        elif opt in ('-c', '--option'):
			if (test_type):
				print "\nPlease supply ONE test type (server OR client)"
				sys.exit(1)	
			test_type = "client"
        elif opt in ('-s', '--option'):
			if (test_type):
				print "\nPlease supply ONE test type (server OR client)"
				sys.exit(1)	
			test_type = "server"          
        elif opt in ('-a', '--address'):
            address = arg
        elif opt in ('-p', '--port'):
            port = arg
        elif opt in ('-t', '--thread'):
            thread = arg
        elif opt in ('-b', '--burst'):
            burst = arg
        elif opt in ('-u', '--cpu'):
            core = arg
        elif opt in ('-f', '--file'):
            file = arg
        elif opt in ('-r', '--runs'):
            runs = arg
        elif opt in ('-i', '--in'):
            in1 = arg
        elif opt in ('-o', '--out'):
            out1 = arg
        else:
            assert False, "unhandled option"
            
if(not test_type):
	print "\nPlease supply the test type (server/client)"
	sys.exit(1)
    
if(not address):
	print "\nPlease supply server address"
	sys.exit(1)

if((not in1 and out1) or (not out1 and in1)):
	print "\nPlease supply either both in and out Message sizes or none of them"
	sys.exit(1)		 

if(not in1):    
	if(test_type == "server"):
		out1 = 0
		in1 = 64
	else:
		in1 = 0
		out1 = 64
		
		
# for MARS usage
cov_command=""
if os.getenv('CODE_COVERAGE_ON') is not None:
	cov_command = '-D' + str(os.getenv('COBERTURA_COVFILE_PATH_PROP_NAME')) + '=' + str(os.getenv('COBERTURA_COVFILE'))

cob_jar_path = os.getenv('COBERTURA_JAR_PATH')
if not cob_jar_path:
	cob_jar_path = ""


print "\nCompiling JAVA files...."
cmd = 'javac -cp "../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." org/accelio/jxio/tests/benchmarks/ServerSessionHandle.java org/accelio/jxio/tests/benchmarks/ServerPortalWorker.java org/accelio/jxio/tests/benchmarks/DataPathTestServer.java org/accelio/jxio/tests/benchmarks/DataPathTestClient.java org/accelio/jxio/tests/benchmarks/ClientWorker.java org/accelio/jxio/tests/benchmarks/DataPathTest.java'
os.system(cmd)
         

if(test_type == "server"):
	print "\n------ Running Server Test Application -----"
	cmd = 'taskset -c %s java -Dlog4j.configuration=org/accelio/jxio/tests/log4j.properties.jxiotest -cp "%s:../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." %s org.accelio.jxio.tests.benchmarks.DataPathTestServer  %s %s %s %s %s %s' % (core, cob_jar_path, cov_command, address, port, thread, in1, out1, burst)
	os.system(cmd)
else:
	print "\n------ Running Client Test Application -----"
	cmd = 'taskset -c %s java -Dlog4j.configuration=org/accelio/jxio/tests/log4j.properties.jxiotest -cp "%s:../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." %s org.accelio.jxio.tests.benchmarks.DataPathTestClient  %s %s %s %s %s %s %s %s' % (core, cob_jar_path, cov_command, address, port, thread, in1, out1, burst, file, runs)
	os.system(cmd)
   
sys.exit(0)
    
