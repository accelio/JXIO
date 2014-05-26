#!/usr/bin/env python

# Built-in modules
import sys

from reg2_wrapper.test_wrapper.client_server_wrapper import ClientServerWrapper
from reg2_wrapper.utils.parser.cmd_argument import RunningStage
from topology.TopologyAPI import TopologyAPI
from reg2_wrapper.players.player import Player

class JxConnectionWrapper(ClientServerWrapper):

    def get_server_prog_path(self):
        return "python ../tests/runControlPathDurationTest.py -s"

    def get_client_prog_path(self):
        return "python ../tests/runControlPathDurationTest.py -c"

    def configure_parser(self):
        super(JxConnectionWrapper, self).configure_parser()
 
        # Arguments
        self.add_dynamic_argument('-a', self.get_server_manage_ip, value_only=False, priority=1)

        self.add_client_cmd_argument('-m_client', type=str, alias='-m', value_only=False, priority=2)
        self.add_client_cmd_argument('-n_client', type=str, alias='-n', value_only=False, priority=3)
        self.add_client_cmd_argument('-p_client', type=str, alias='-p', value_only=False, priority=4)

        self.add_server_cmd_argument('-m_server', type=str, alias='-m', value_only=False, priority=2)
        self.add_server_cmd_argument('-n_server', type=str, alias='-n', value_only=False, priority=3)
        self.add_server_cmd_argument('-p_server', type=str, alias='-p', value_only=False, priority=4)

    def get_server_manage_ip(self):
	 # Get Topology to find ib IP
	 self.topology_api = TopologyAPI(self.topo_file)
         hosts = self.topology_api.get_all_hosts()
	 for host in hosts:
            base_ip = self.topology_api.get_object_attribute(host, "BASE_IP")
	    # Check if server IP is the current host
	    if self.ServerPlayer.Ip == base_ip:
               ports = self.topology_api.get_device_active_ports(host)
               ip = self.topology_api.get_port_ip(ports[0])
	       return ip
         return self.ServerPlayer.Ip
# 
#     def get_client_manage_ip(self):
#         return self.ClientPlayers[0].Ip
# 
#     def get_client_test_ip(self):
#         return self.ClientEPoints[0].ipv4
                              
if __name__ == "__main__":
    wrapper = JxConnectionWrapper("JX Wrapper")
    wrapper.execute(sys.argv[1:])

