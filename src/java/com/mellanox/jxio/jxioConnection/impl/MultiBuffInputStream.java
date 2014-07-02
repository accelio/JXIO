package com.mellanox.jxio.jxioConnection.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

//inspired from SocketInputStream 
public class MultiBuffInputStream extends InputStream {

	private final BufferSupplier supplier;
	private ByteBuffer           inputBuff      = null;
	private boolean              eof            = false;
	private boolean              connectionOpen = true;

	public MultiBuffInputStream(BufferSupplier sup) {
		supplier = sup;
	}

	@Override
	public synchronized int read() throws IOException {
		byte[] temp = new byte[1];
		int n = read(temp, 0, 1);
		if (n <= 0) {
			;
			return -1;
		}
		return temp[0] & 0xFF;
	}

	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		int totalRead = 0;
		int bytesRead;

		if (!connectionOpen) {
			throw new IOException("Stream closed.");
		}
		if (eof) {
			return -1;
		}
		if (b == null) {
			throw new NullPointerException();
		}
		// bounds check
		if (len <= 0 || off < 0 || off + len > b.length) {
			if (len == 0) {
				return 0;
			}
			throw new ArrayIndexOutOfBoundsException();
		}
		if (inputBuff == null) {
			inputBuff = supplier.getNextBuffer();
		}
		while (totalRead < len) {
			// bring new buffer if empty
			if (!inputBuff.hasRemaining()) {
				inputBuff = supplier.getNextBuffer();
			}
			bytesRead = Math.min(len - totalRead, inputBuff.remaining());
			inputBuff.get(b, off, bytesRead);
			totalRead += bytesRead;
			off += bytesRead;
			if (inputBuff.limit() <= inputBuff.position() && inputBuff.position() != inputBuff.capacity()) {
				eof = true;
				return totalRead;
			}
		}
		return totalRead;
	}

	@Override
	public synchronized int available() throws IOException {
		if (!connectionOpen) {
			throw new IOException("Stream closed.");
		}
		return inputBuff.remaining();
	}

	@Override
	public void close() throws IOException {
		connectionOpen = false;
	}

	@Override
	public synchronized long skip(long n) throws IOException {
		long numSkipped = 0;
		long skip;

		if (!connectionOpen) {
			throw new IOException("Stream closed.");
		}
		if (eof || n <= 0) {
			return 0;
		}
		if (inputBuff == null) {
			inputBuff = supplier.getNextBuffer();
		}
		while (numSkipped < n) {
			// bring new buffer if empty
			if (!inputBuff.hasRemaining()) {
				inputBuff = supplier.getNextBuffer();
			}
			skip = Math.min(n - numSkipped, inputBuff.remaining());
			inputBuff.position((int) (inputBuff.position() + skip));
			numSkipped += skip;
		}
		return numSkipped;
	}

}
