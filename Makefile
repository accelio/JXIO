

TARGET=jx.jar
JX_SO=libjx.so

FILES=java/src/com/mellanox/*.java \

all: $(TARGET)

$(TARGET):$(FILES)
	(cd libtrdma/;./configure && make)
	(cd c;./configure && make)
	rm -rf java/bin/*
	javac -d java/bin/ $(FILES)
	(cd java/bin ;jar -cvf $(TARGET) com)
	cp java/bin/$(TARGET) tests/
	cp c/src/libjx.so java/bin
	cp c/src/libjx.so tests/
	cp libtrdma/lib/libxio.so java/bin
	cp libtrdma/lib/libxio.so tests/ 
clean:
	(cd c; make distclean)
	rm -rf java/bin/*
	rm -rf tests/$(TARGET)
