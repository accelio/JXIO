package com.mellanox.jxio.jxioConnection.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface BufferSupplier {

	ByteBuffer getNextBuffer() throws IOException;
}
