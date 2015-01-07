
# Configuring Running Directory
TOP_DIR=.

GIT_VERSION=`git describe --long --tags --always --dirty`

# Configuring Parameters
TARGET=jxio.jar
BIN_FOLDER=$(TOP_DIR)/bin
LIB_FOLDER=$(TOP_DIR)/src/lib
SRC_JAVA_FOLDER=$(TOP_DIR)/src/java
SRC_JAVA_FILES=$(SRC_JAVA_FOLDER)/org/accelio/jxio/*.java $(SRC_JAVA_FOLDER)/org/accelio/jxio/exceptions/*.java $(SRC_JAVA_FOLDER)/org/accelio/jxio/impl/*.java $(SRC_JAVA_FOLDER)/org/accelio/jxio/jxioConnection/*.java $(SRC_JAVA_FOLDER)/org/accelio/jxio/jxioConnection/impl/*.java $(SRC_JAVA_FOLDER)/org/apache/lucene/facet/taxonomy/LRUHashMap.java

NATIVE_LIBS=libjxio.so libxio.so

STRIP_COMMAND=touch #do not strip libraries from symbols

ifndef DONT_STRIP 
	STRIP_COMMAND=strip -s
endif
 
all: $(TARGET)

$(TARGET):$(SRC_JAVA_FILES)
	rm -rf $(BIN_FOLDER)/*; mkdir -p $(BIN_FOLDER)
	(cd src/accelio/; make -s)
	cp src/accelio/src/usr/.libs/libxio.so $(BIN_FOLDER)
	$(STRIP_COMMAND) $(BIN_FOLDER)/libxio.so
	(cd src/c; make -s)
	cp src/c/src/.libs/libjxio.so $(BIN_FOLDER)
	$(STRIP_COMMAND) $(BIN_FOLDER)/libjxio.so
	javac -cp $(LIB_FOLDER)/commons-logging.jar -d $(BIN_FOLDER) $(SRC_JAVA_FILES)
	(echo $(GIT_VERSION) > version)
	(cp manifest.template manifest.txt; sed -i "s/Implementation-Version: .*/Implementation-Version: $(GIT_VERSION)/" manifest.txt)
	(echo "Implementation-Version-AccelIO: `cd src/accelio; git describe --long --tags --always --dirty; cd ../..`" >> manifest.txt)
	(cd $(BIN_FOLDER); jar -cfm $(TARGET) ../manifest.txt org $(NATIVE_LIBS))

clean:
	(cd src/c; make clean -s)
	rm -rf $(BIN_FOLDER)/*
