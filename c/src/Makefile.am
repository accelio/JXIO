# this is example-file: src/Makefile.am

JDK = $(JAVA_HOME)
topdir=../../

# additi-onal include paths necessary to compile the C library
AM_CPPFLAGS = -fPIC -DPIC  				\
		-I$(JDK)/include -I$(JDK)/include/linux \
		-I$(topdir)/accelio/include
	    @AM_CFLAGS@
AM_LDFLAGS = -lxio -lrdmacm -libverbs -lrt -ldl \
			 -L$(topdir)accelio/src

###############################################################################
# THE LIBRARIES TO BUILD
###############################################################################

# the library names to build (note we are building static libs only)
bin_PROGRAMS = libjx.so

# the sources to add to the library and to add to the source distribution
libjx_so_SOURCES = Utils.cc Event_queue.cc Events.cc CallbackFunctions.cc Server.cc Client.cc Context.cc Msg.cc MsgPool.cc Bridge.cc
	
				
libjx_so_LDFLAGS = -shared -rdynamic 		\
				     -L$(topdir)accelio/src/usr/ -lxio -lrdmacm -libverbs -lrt -ldl

libjx_so_LDADD = $(AM_LDFLAGS)

				
###############################################################################
