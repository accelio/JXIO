package com.mellanox.jxio.tests.benchmarks.jxioConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.jxioConnection.JxioConnectionServer;

public class UserServerCallbacks implements JxioConnectionServer.Callbacks {

	private static final Log LOG  = LogFactory.getLog(UserServerCallbacks.class.getCanonicalName());
	private final byte[]     temp = new byte[JxioConnectionServer.msgPoolBuffSize];

	public void newSessionOS(URI uri, OutputStream output) {
		long bytes = getBytes(uri);
		LOG.info(Thread.currentThread().toString()+" going to send " + bytes + " bytes");
		// outStream.skip(bytesToRead);
		long sent = 0;
		try {
			while (sent < bytes) {
				output.write(temp);
				sent += temp.length;
			}
			output.close();
		} catch (IOException e) {
			LOG.error(Thread.currentThread().toString()+" Error reading data, "+e.getMessage());
			e.printStackTrace();
		}

	}

	@Override
	public void newSessionIS(URI uri, InputStream input) {
		long bytes = getBytes(uri);
		LOG.info(Thread.currentThread().toString()+" going to read " + bytes + " bytes");
		long read = 0;
		try {
			while (read < bytes) {
				input.read(temp);
				read += temp.length;
			}
			input.close();
		} catch (IOException e) {
			LOG.error(Thread.currentThread().toString()+" Error reading data, "+e.getMessage());
			e.printStackTrace();
		}
	}

	public long getBytes(URI uri) {
		String query = uri.getQuery();
		return Long.parseLong(query.split("size=")[1].split("&")[0]);
	}
}
