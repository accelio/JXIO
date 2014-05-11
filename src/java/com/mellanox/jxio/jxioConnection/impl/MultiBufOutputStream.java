package com.mellanox.jxio.jxioConnection.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MultiBufOutputStream extends OutputStream {

	private static final Log    LOG            = LogFactory.getLog(MultiBufOutputStream.class);
	private final BufferManager bufManager;
	private ByteBuffer          outBuf         = null;
	private boolean             connectionOpen = true;

	public MultiBufOutputStream(BufferManager man) {
		bufManager = man;
	}

	@Override
	public synchronized void write(int b) throws IOException {
		byte[] temp = new byte[1];
		write(temp, 0, 1);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		int totalWrite = 0;
		int bytesWrite;

		if (!connectionOpen) {
			throw new IOException("Stream closed.");
		}
		if (b == null) {
			throw new NullPointerException();
		}
		// bounds check
		if (len < 0 || off < 0 || off + len > b.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (outBuf == null) {
			outBuf = bufManager.getNextBuffer();
		}
		while (totalWrite < len) {
			// bring new buffer if full
			if (!outBuf.hasRemaining()) {
				outBuf = bufManager.getNextBuffer();
			}
			bytesWrite = Math.min(len - totalWrite, outBuf.remaining());
			outBuf.put(b, off, bytesWrite);
			totalWrite += bytesWrite;
			off += bytesWrite;
		}
	}

	@Override
	public synchronized void flush() throws IOException {
		bufManager.flush();
		outBuf = null;
	}

	@Override
	public void close() throws IOException {
		flush();
		connectionOpen = false;
		bufManager.close();
	}

	// FOR TESTING!
	public synchronized long skip(long n) throws IOException {
		long numSkipped = 0;
		long skip;

		if (outBuf == null) {
			outBuf = bufManager.getNextBuffer();
		}
		while (numSkipped < n) {
			// bring new buffer if empty
			if (!outBuf.hasRemaining()) {
				outBuf = bufManager.getNextBuffer();
			}
			skip = Math.min(n - numSkipped, outBuf.remaining());
			outBuf.position(outBuf.position() + (int) skip);
			numSkipped += skip;
		}
		return numSkipped;
	}
}
