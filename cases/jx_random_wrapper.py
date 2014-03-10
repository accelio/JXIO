#!/usr/bin/env python

# Built-in modules
import sys
import subprocess
import os
import getpass
import socket

from reg2_wrapper.test_wrapper.standalone_wrapper import StandaloneWrapper
from reg2_wrapper.utils.parser.cmd_argument import RunningStage
from topology.TopologyAPI import TopologyAPI
from reg2_wrapper.players.player import Player

class JxRandomWrapper(StandaloneWrapper):

    def run_pre_commands(self):
	# Define needed parameters
        here = os.path.dirname(os.path.abspath(__file__))
	print("Running directory is " + here + "!")
	print("Running user is " + getpass.getuser() + "!")
	my_topology = "/tmp/my_topology.xml"
        self.topology_api = TopologyAPI("/.autodirect/mtrswgwork/alongr/mars/MARS_conf/topo/JX-Setup/topology.xml")
        hosts = self.topology_api.get_all_hosts()
	# Create the topology file
	f = open(my_topology, "w")
        f.write("\t<machines>\n")
	amount = len(hosts)
	f.write("\t\t<machine_amount value=\"%s\"/>\n" % amount)
        for host in hosts:
            f.write("\t\t<machine>\n")
            ip = self.topology_api.get_object_attribute(host, "BASE_IP")
            conn_type = self.topology_api.get_object_attribute(host, "HOST_PORT_ETH")
            if conn_type != "eth":
                ip = ip.replace("172.30.21.", "36.0.0.")
            hostname = socket.gethostbyaddr(ip)[0].replace("-", "_")
            f.write("\t\t\t<name value=\"%s\"/>\n" %hostname)
            f.write("\t\t\t<address value=\"%s\"/>\n" %ip)
            f.write("\t\t\t<type value=\"%s\"/>\n" %conn_type)
            f.write("\t\t</machine>\n")
        f.write("\t</machines>\n")
	f.close()
	# Inject the new topology to probability file
        subprocess.call("./DefineTopology.sh ../tests/com/mellanox/jxio/tests/random/ probability.xml " + my_topology, shell=True, cwd=here)
        subprocess.call("./DefineTopology.sh ../tests/com/mellanox/jxio/tests/random/ probability_simple.xml " + my_topology, shell=True, cwd=here)
        subprocess.call("./DefineTopology.sh ../tests/com/mellanox/jxio/tests/random/ probability_long_dur_client_long.xml " + my_topology, shell=True, cwd=here)
        subprocess.call("./DefineTopology.sh ../tests/com/mellanox/jxio/tests/random/ probability_client_repeats.xml " + my_topology, shell=True, cwd=here)
	# Copy the new probability xml to players
        for host in hosts:
		ip = self.topology_api.get_object_attribute(host, "BASE_IP")
		print("Copying topology to " + ip + "!")
		subprocess.call("scp ../tests/com/mellanox/jxio/tests/random/probability.xml root@" + ip + ":/tmp/mars_tests/UDA-jx.db/tests/tests/com/mellanox/jxio/tests/random/probability.xml", shell=True, cwd=here)
		subprocess.call("scp ../tests/com/mellanox/jxio/tests/random/probability_simple.xml root@" + ip + ":/tmp/mars_tests/UDA-jx.db/tests/tests/com/mellanox/jxio/tests/random/probability_simple.xml", shell=True, cwd=here)
		subprocess.call("scp ../tests/com/mellanox/jxio/tests/random/probability_long_dur_client_long.xml root@" + ip + ":/tmp/mars_tests/UDA-jx.db/tests/tests/com/mellanox/jxio/tests/random/probability_long_dur_client_long.xml", shell=True, cwd=here)
		subprocess.call("scp ../tests/com/mellanox/jxio/tests/random/probability_client_repeats.xml root@" + ip + ":/tmp/mars_tests/UDA-jx.db/tests/tests/com/mellanox/jxio/tests/random/probability_client_repeats.xml", shell=True, cwd=here)

    def get_prog_path(self, running_stage=RunningStage.RUN):
        return "../tests/runRandomTest.sh"

    def configure_parser(self):
        super(JxRandomWrapper, self).configure_parser()

        # Arguments
        self.add_cmd_argument('--path', help='The path to the probability file.', type=str, value_only=True, priority=1, action='store', required=True)
        self.add_cmd_argument('--filename', help='The probabilty file name.', type=str, value_only=True, priority=2, action='store', required=True)
	self.add_test_attribute_argument('', 'seed', priority=3)
        self.add_cmd_argument('--timeout', help='The timeout in seconds for the entire run.', type=str, value_only=True, priority=4, action='store', required=True)

if __name__ == "__main__":
    wrapper = JxRandomWrapper("JX Random Wrapper")
    wrapper.execute(sys.argv[1:])
