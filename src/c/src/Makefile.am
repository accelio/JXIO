JDK = $(JAVA_HOME)
topdir=../../

# additional include paths necessary to compile the C library
AM_CPPFLAGS = -fPIC -DPIC -DGIT_VERSION=\"`git describe --long --always`\" -Werror \
		-I$(JDK)/include -I$(JDK)/include/linux \
		-I$(topdir)/accelio/include
	    @AM_CFLAGS@
AM_LDFLAGS = -lxio -lrdmacm -libverbs -lrt -ldl \
			 -L$(topdir)accelio/src

###############################################################################
# THE LIBRARIES TO BUILD
###############################################################################

# the library names to build (note we are building static libs only)
bin_PROGRAMS = libjxio.so

# the sources to add to the library and to add to the source distribution
libjxio_so_SOURCES = Utils.cc EventQueue.cc Events.cc CallbackFunctionsServer.cc CallbackFunctionsClient.cc ServerPortal.cc Client.cc Context.cc Msg.cc MsgPool.cc MsgPools.cc ServerSession.cc Bridge.cc


libjxio_so_LDFLAGS = -shared -rdynamic -L$(topdir)accelio/src/usr/.libs -lxio -lrdmacm -libverbs -lrt -ldl

libjxio_so_LDADD = $(AM_LDFLAGS)

cov:
	PATH=/.autodirect/app/Coverity/cov-analysis-linux64-7.0.1/bin:$$PATH && rm -rf $(PWD)/cov-build && make clean && cov-build --dir $(PWD)/cov-build make all && cov-analyze --dir $(PWD)/cov-build && cov-format-errors --dir $(PWD)/cov-build --html-output $(PWD)/cov-build/c/output/errors/

clean:
	rm -rf *.o

###############################################################################
