
# Configuring Running Directory
TOP_DIR=.


# Configuring Parameters
TARGET=jxio.jar
BIN_FOLDER=$(TOP_DIR)/bin
LIB_FOLDER=$(TOP_DIR)/src/lib
SRC_JAVA_FOLDER=$(TOP_DIR)/src/java
SRC_JAVA_FILES=$(SRC_JAVA_FOLDER)/com/mellanox/jxio/*.java $(SRC_JAVA_FOLDER)/com/mellanox/jxio/exceptions/*.java $(SRC_JAVA_FOLDER)/com/mellanox/jxio/impl/*.java $(SRC_JAVA_FOLDER)/com/mellanox/jxio/jxioConnection/*.java $(SRC_JAVA_FOLDER)/com/mellanox/jxio/jxioConnection/impl/*.java $(SRC_JAVA_FOLDER)/org/apache/lucene/facet/taxonomy/LRUHashMap.java

NATIVE_LIBS=libjxio.so libxio.so
 
all: $(TARGET)

$(TARGET):$(SRC_JAVA_FILES)
	rm -rf $(BIN_FOLDER)/*
	(cd src/accelio/; make -s)
	cp src/accelio/src/usr/.libs/libxio.so $(BIN_FOLDER)
	(cd src/c; make -s)
	cp src/c/src/libjxio.so $(BIN_FOLDER)
	javac -cp $(LIB_FOLDER)/commons-logging.jar -d $(BIN_FOLDER) $(SRC_JAVA_FILES)
	(cd $(BIN_FOLDER); jar -cfm $(TARGET) ../manifest.txt com org $(NATIVE_LIBS))

clean:
	(cd src/c; make clean -s)
	rm -rf $(BIN_FOLDER)/*
