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
    print "Usage: ./tests/runControlPathDurationTest.py OPTION"
    print "\t-h  | --help		print help"
    print "\t-c  | --client		run a client test"
    print "\t-s  | --server		run a server test"
    print "\t-a  | --address		Server's address"
    print "\t-p  | --port		Server's port (default is 2222)"
    print "\t-m  | --multi_thread	0 single thread, 1 for multi thread (default 0)"
    print "\t-n  | --number_clients	number of clients (and servers for multi thread options)"   
    print "examples :"
    print "\ttests/runControlPathDurationTest.py -c -a 1.1.1.1 -p 2222 -m 0 -n 1"  
    print "\ttests/runControlPathDurationTest.py -s -a 1.1.1.1 -p 2222 -m 0 -n 1"   


options, remainder = getopt.gnu_getopt(sys.argv[1:], '?hcsa:p:m:n:', ['help',
                                                                'address=',
                                                                'port=',
                                                                'multi_thread=',
                                                                'number_clients='])
                                                                
test_type = None
address = None
num_clients = 1
port = 2222
multi_thread = 0

                                                              
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
        elif opt in ('-m', '--multi_thread'):
            multi_thread = arg
        elif opt in ('-n', '--number_clients'):
            num_clients = arg
        else:
            assert False, "unhandled option"
            
if(not test_type):
	print "\nPlease supply the test type (server/client)"
	sys.exit(1)
    
if(not address):
	print "\nPlease supply server address"
	sys.exit(1)
		
# for MARS usage
cov_command=""
if os.getenv('CODE_COVERAGE_ON') is not None:
	cov_command = '-D' + str(os.getenv('COBERTURA_COVFILE_PATH_PROP_NAME')) + '=' + str(os.getenv('COBERTURA_COVFILE'))

cob_jar_path = os.getenv('COBERTURA_JAR_PATH')
if not cob_jar_path:
	cob_jar_path = ""


print "\nCompiling JAVA files...."
cmd = 'javac -cp "../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." com/mellanox/jxio/tests/controlPathDuration/CPDClientWorkerThread.java com/mellanox/jxio/tests/controlPathDuration/CPDServerPortalWorker.java  com/mellanox/jxio/tests/controlPathDuration/CPDTestClient.java com/mellanox/jxio/tests/controlPathDuration/CPDTestServer.java'
os.system(cmd)
         

if(test_type == "server"):
	print "\n------ Running Server Test Application -----"
	cmd = 'java -Dlog4j.configuration=com/mellanox/jxio/tests/log4j.properties.jxiotest -cp ":../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." com.mellanox.jxio.tests.controlPathDuration.CPDTestServer  %s %s %s %s' % (address, port, multi_thread, num_clients)
	os.system(cmd)
else:
	print "\n------ Running Client Test Application -----"
	cmd = 'java -Dlog4j.configuration=com/mellanox/jxio/tests/log4j.properties.jxiotest -cp ":../bin/jxio.jar:../src/lib/commons-logging.jar:../src/lib/log4j-1.2.15.jar:." com.mellanox.jxio.tests.controlPathDuration.CPDTestClient  %s %s %s %s' % (address, port, multi_thread, num_clients)
	os.system(cmd)
   
sys.exit(0)
    
