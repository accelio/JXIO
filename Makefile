

TARGET=jx.jar
JX_SO=libjx.so
JAVA_FILES=java/src/com/mellanox/jxio/*.java java/src/com/mellanox/jxio/impl/*.java java/src/com/mellanox/jxio/tests/*.java


all: $(TARGET)

$(TARGET):$(JAVA_FILES)
	(cd accelio/; make)
	(cd c; make)
	rm -rf java/bin/*
	javac -d java/bin/ $(JAVA_FILES)
	(cd java/bin ;jar -cvf $(TARGET) com)
	cp java/bin/$(TARGET) java/src/com/mellanox/jxio/tests/
	cp c/src/libjx.so java/bin
	cp c/src/libjx.so java/src/com/mellanox/jxio/tests/
	cp  accelio/src/usr/libxio.so java/bin
	cp  accelio/src/usr/libxio.so java/src/com/mellanox/jxio/tests/ 
clean:
	(cd c; make distclean)
	rm -rf java/bin/*
	rm -rf java/src/com/mellanox/jxio/tests/$(TARGET)
