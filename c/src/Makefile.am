# this is example-file: src/Makefile.am

JDK = $(JAVA_HOME)
topdir=../../

# additi-onal include paths necessary to compile the C library
AM_CPPFLAGS = -fPIC -DPIC  				\
		-I$(JDK)/include -I$(JDK)/include/linux \
		-I$(topdir)/libtrdma/include  
	    @AM_CFLAGS@
AM_LDFLAGS = -lxio -lrdmacm -libverbs -lrt -ldl \
			 -L$(topdir)libtrdma/src 

###############################################################################
# THE LIBRARIES TO BUILD
###############################################################################

# the library names to build (note we are building static libs only)
bin_PROGRAMS = libjx.so

# the sources to add to the library and to add to the source distribution
libjx_so_SOURCES = cJXBridge.cc
	
				
libjx_so_LDFLAGS = -shared -rdynamic 		\
				     -L$(topdir)libtrdma/lib -lxio -lrdmacm -libverbs -lrt -ldl

libjx_so_LDADD = $(AM_LDFLAGS)

				
###############################################################################
