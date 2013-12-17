#!/usr/bin/env python

# Built-in modules
import sys

from reg2_wrapper.test_wrapper.standalone_wrapper import StandaloneWrapper
from reg2_wrapper.utils.parser.cmd_argument import RunningStage

class JxioTestsWrapper(StandaloneWrapper):

    def get_command(self, running_stage=RunningStage.RUN):
        return "../tests/runBreakEventLoopTest.sh"

if __name__ == "__main__":
    wrapper = JxioTestsWrapper("JXIO Tests Wrapper")
    wrapper.execute(sys.argv[1:])
