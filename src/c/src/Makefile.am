JDK = $(JAVA_HOME)
topdir=../../

# additional include paths necessary to compile the C library
AM_CPPFLAGS = -fPIC -DPIC -DGIT_VERSION=\"`git describe --long --tags --always --dirty`\" -Werror \
		-I$(JDK)/include -I$(JDK)/include/linux \
		-I$(topdir)/accelio/include

###############################################################################
# THE LIBRARIES TO BUILD
###############################################################################

lib_LTLIBRARIES = libjxio.la

libjxio_la_SOURCES = Utils.cc EventQueue.cc Events.cc CallbackFunctionsServer.cc CallbackFunctionsClient.cc ServerPortal.cc Client.cc Context.cc Msg.cc MsgPool.cc MsgPools.cc ServerSession.cc Bridge.cc

libjxio_la_LDFLAGS = -shared -rdynamic

libjxio_la_LIBADD = -lxio -L$(topdir)accelio/src/usr

libjxio_la_DEPENDENCIES = Makefile.am Makefile.in Makefile

cov:
	PATH=/.autodirect/app/Coverity/cov-analysis-linux64-7.0.1/bin:$$PATH && rm -rf $(PWD)/cov-build && make clean && cov-build --dir $(PWD)/cov-build make all && cov-analyze --dir $(PWD)/cov-build && cov-format-errors --dir $(PWD)/cov-build --html-output $(PWD)/cov-build/c/output/errors/

clean:
	rm -rf *.o *.lo *.la

###############################################################################
