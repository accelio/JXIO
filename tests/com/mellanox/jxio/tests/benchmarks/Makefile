CC=gcc
.PATH: ${.CURDIR}../../../../../../accelio/include/
CFLAGS+= -O3 -I ${.CURDIR}../../../../../../accelio/include/

LIBS=-L../../../../../../accelio/src/usr/.libs/ -lxio -libverbs -lrdmacm -lrt
	
all: xio_server xio_client_stat

xio_client_stat: xio_client_stat.o
	 $(CC) $(CFLAGS) $(LIBS) -o xio_client_stat xio_client_stat.o

xio_server: xio_server.o
	$(CC) $(CFLAGS) $(LIBS) -o xio_server xio_server.o

	
clean:
	rm -rf *.o 
