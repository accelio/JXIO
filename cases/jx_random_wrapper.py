#!/usr/bin/env python

# Built-in modules
import sys

from reg2_wrapper.test_wrapper.standalone_wrapper import StandaloneWrapper
from reg2_wrapper.utils.parser.cmd_argument import RunningStage

class JxRandomWrapper(StandaloneWrapper):

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
