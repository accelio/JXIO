#!/usr/bin/env python

# Built-in modules
import sys

from reg2_wrapper.test_wrapper.client_server_wrapper import ClientServerWrapper
from reg2_wrapper.utils.parser.cmd_argument import RunningStage
from topology.TopologyAPI import TopologyAPI
from reg2_wrapper.players.player import Player

class JxConnectionWrapper(ClientServerWrapper):

    def get_server_prog_path(self):
        return "bash ../tests/runJxioConnectionTest.sh s"

    def get_client_prog_path(self):
        return "bash ../tests/runJxioConnectionTest.sh c"

    def configure_parser(self):
        super(JxConnectionWrapper, self).configure_parser()
 
        # Arguments
        self.add_dynamic_argument('-ip', self.get_server_manage_ip, value_only=True, priority=1)

        self.add_client_cmd_argument('-port_client', help='The port.', type=str, alias='-port_client', value_only=True, priority=2)
        self.add_client_cmd_argument('-server_buffer_count_client', help='The server buffer count.', type=str, alias='-server_buffer_count_client', value_only=True, priority=3)
        self.add_client_cmd_argument('-num_workers_client', help='The number of workers.', type=str, alias='-num_workers_client', value_only=True, priority=4)
        self.add_client_cmd_argument('-data_size_client', help='The size of data in bytes.', type=str, alias='-data_size_client', value_only=True, priority=5)
        self.add_client_cmd_argument('-client_buffer_count_client', help='The client buffer count.', type=str, alias='-client_buffer_count_client', value_only=True, priority=6)
        self.add_client_cmd_argument('-num_clients_client', help='The number of clients.', type=str, alias='-num_clients_client', value_only=True, priority=7)
        self.add_client_cmd_argument('-num_times_client', help='The number of times to run test.', type=str, alias='-num_times_client', value_only=True, priority=8)

        self.add_server_cmd_argument('-port_server', help='The port.', type=str, alias='-port_server', value_only=True, priority=2)
        self.add_server_cmd_argument('-server_buffer_count_server', help='The server buffer count.', type=str, alias='-server_buffer_count_server', value_only=True, priority=3)
        self.add_server_cmd_argument('-num_workers_server', help='The number of workers.', type=str, alias='-num_workers_server', value_only=True, priority=4)
        self.add_server_cmd_argument('-data_size_server', help='The size of data in bytes.', type=str, alias='-data_size_server', value_only=True, priority=5)
        self.add_server_cmd_argument('-client_buffer_count_server', help='The client buffer count.', type=str, alias='-client_buffer_count_server', value_only=True, priority=6)
        self.add_server_cmd_argument('-num_clients_server', help='The number of clients.', type=str, alias='-num_clients_server', value_only=True, priority=7)
        self.add_server_cmd_argument('-num_times_server', help='The number of times to run test.', type=str, alias='-num_times_server', value_only=True, priority=8)

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

