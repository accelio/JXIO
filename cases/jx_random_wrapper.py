#!/usr/bin/env python

# Built-in modules
import sys

from reg2_wrapper.test_wrapper.client_server_wrapper import ClientServerWrapper

class JxWrapper(ClientServerWrapper):

    def get_server_prog_path(self):
        return "echo server"

    def get_client_prog_path(self):
        return "../tests/runRandomTest.sh"

    def configure_parser(self):
        super(JxWrapper, self).configure_parser()
 
        # Arguments
        self.add_client_cmd_argument('--path', help='The path to the probability file.', type=str, value_only=True, priority=1)
        self.add_client_cmd_argument('--filename', help='The probabilty file name.', type=str, value_only=True, priority=2)
        self.add_client_cmd_argument('--random_seed', help='The fixed random seed. Set to 0 to randimize a seed.', type=str, value_only=True, priority=3)
        self.add_client_cmd_argument('--timeout', help='The timeout in seconds for the entire run.', type=str, value_only=True, priority=4)


#     def get_server_manage_ip(self):
#         return self.ServerPlayer.Ip
# 
#     def get_client_manage_ip(self):
#         return self.ClientPlayers[0].Ip
# 
#     def get_client_test_ip(self):
#         return self.ClientEPoints[0].ipv4
                              
if __name__ == "__main__":
    wrapper = JxWrapper("JX Wrapper")
    wrapper.execute(sys.argv[1:])

