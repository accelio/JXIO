package com.mellanox.jxio.jxioConnection.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface BufferManager {
	
	void flush();

	void close();

	ByteBuffer getNextBuffer() throws IOException;

}
