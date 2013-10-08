
# Configuring Running Directory
TOP_DIR=.


# Configuring Parameters
TARGET=jxio.jar
BIN_FOLDER=$(TOP_DIR)/bin
SRC_JAVA_FOLDER=$(TOP_DIR)/java/src
SRC_JAVA_FILES=$(SRC_JAVA_FOLDER)/com/mellanox/jxio/*.java $(SRC_JAVA_FOLDER)/com/mellanox/jxio/impl/*.java

NATIVE_LIBS=libjxio.so libxio.so
 
all: $(TARGET)

$(TARGET):$(SRC_JAVA_FILES)
	rm -rf $(BIN_FOLDER)/*
	(cd accelio/; make)
	cp accelio/src/usr/.libs/libxio.so $(BIN_FOLDER)
	(cd c; make)
	cp c/src/libjxio.so $(BIN_FOLDER)
	javac -cp lib/commons-logging.jar -d $(BIN_FOLDER) $(SRC_JAVA_FILES)
	(cd $(BIN_FOLDER) ;jar -cvf $(TARGET) com $(NATIVE_LIBS))

clean:
	(cd c; make clean)
	rm -rf $(BIN_FOLDER)/*
