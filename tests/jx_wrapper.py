#!/usr/bin/env python

# Built-in modules
import sys

from reg2_wrapper.test_wrapper.client_server_wrapper import ClientServerWrapper

class JxWrapper(ClientServerWrapper):

    def get_server_prog_path(self):
        return "../libxio/tests/usr/hello_test/run_server.sh"

    def get_client_prog_path(self):
        return "../tests/runClientTest.sh"

    def configure_parser(self):
        super(JxWrapper, self).configure_parser()
 
        # Arguments
        self.add_client_cmd_argument('--host', help='The host ip or name of the server.', type=str, value_only=True, priority=1)
        self.add_client_cmd_argument('--port', help='Port number of the server.', type=int, value_only=True, priority=2)
        self.add_client_cmd_argument('--test_num', help='The test number to run.', type=int, value_only=True, priority=3)

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

