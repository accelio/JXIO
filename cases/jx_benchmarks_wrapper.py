#!/usr/bin/env python

# Built-in modules
import sys

from reg2_wrapper.test_wrapper.client_server_wrapper import ClientServerWrapper

class JxBenchmarksWrapper(ClientServerWrapper):

    def get_server_prog_path(self):
        return "bash ./runBenchmarkTest.sh 150 -s"

    def get_client_prog_path(self):
        return "bash ./runBenchmarkTest.sh 150 -c"

    def configure_parser(self):
        super(JxBenchmarksWrapper, self).configure_parser()
 
        # Arguments
        self.add_dynamic_argument('-a', self.get_server_manage_ip, value_only=True, priority=1)

        self.add_client_cmd_argument('-p_client', help='The port.', type=str, alias='-p', value_only=True, priority=2)
        self.add_client_cmd_argument('-t_client', help='t.', type=str, alias='-t', value_only=True, priority=3)
        self.add_client_cmd_argument('-i_client', help='i.', type=str, alias='-i', value_only=True, priority=4)
        self.add_client_cmd_argument('-o_client', help='o.', type=str, alias='-o', value_only=True, priority=5)
        self.add_client_cmd_argument('-u_client', help='u.', type=str, alias='-u', value_only=True, priority=6)
        self.add_client_cmd_argument('-r_client', help='r.', type=str, alias='-r', value_only=True, priority=7)

        self.add_server_cmd_argument('-p_server', help='The port.', type=str, alias='-p', value_only=True, priority=2)
        self.add_server_cmd_argument('-t_server', help='t.', type=str, alias='-t', value_only=True, priority=3)
        self.add_server_cmd_argument('-i_server', help='i.', type=str, alias='-i', value_only=True, priority=4)
        self.add_server_cmd_argument('-o_server', help='o.', type=str, alias='-o', value_only=True, priority=5)
        self.add_server_cmd_argument('-u_server', help='u.', type=str, alias='-u', value_only=True, priority=6)
        self.add_server_cmd_argument('-r_server', help='r.', type=str, alias='-r', value_only=True, priority=7)


    def get_server_manage_ip(self):
         return self.ServerPlayer.Ip
# 
#     def get_client_manage_ip(self):
#         return self.ClientPlayers[0].Ip
# 
#     def get_client_test_ip(self):
#         return self.ClientEPoints[0].ipv4
                              
if __name__ == "__main__":
    wrapper = JxBenchmarksWrapper("JX Wrapper")
    wrapper.execute(sys.argv[1:])

