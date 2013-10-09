

TARGET=jx.jar
JX_SO=libjx.so
JAVA_FILES=java/src/com/mellanox/jxio/*.java java/src/com/mellanox/jxio/impl/*.java tests/com/mellanox/jxio/tests/*.java


all: $(TARGET)

$(TARGET):$(JAVA_FILES)
	(cd accelio/; make)
	(cd c; make)
	rm -rf java/bin/*
	javac -cp lib/commons-logging.jar -d java/bin/ $(JAVA_FILES)
	(cd java/bin ;jar -cvf $(TARGET) com)
	cp java/bin/$(TARGET) tests/com/mellanox/jxio/tests/
	cp c/src/libjx.so java/bin
	cp c/src/libjx.so tests/com/mellanox/jxio/tests/
	cp  accelio/src/usr/.libs/libxio.so java/bin
	cp  accelio/src/usr/.libs/libxio.so tests/com/mellanox/jxio/tests/ 
clean:
	(cd c; make distclean)
	rm -rf java/bin/*
	rm -rf tests/com/mellanox/jxio/tests/$(TARGET)
