package com.mellanox.jxio.tests.benchmarks.jxioConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.jxioConnection.JxioConnectionServer;

public class UserServerCallbacks implements JxioConnectionServer.Callbacks {

	private static final Log LOG = LogFactory.getLog(UserServerCallbacks.class.getCanonicalName());

	public void newSessionOS(URI uri, OutputStream output) {
		byte[] temp = new byte[JxioConnectionServer.msgPoolBuffSize];
		long bytes = getBytes(uri);
		LOG.info(Thread.currentThread().toString() + " going to send " + bytes + " bytes");
		long sent = 0;
		int num = 0;
		try {
			while (sent < bytes) {
				num = (int) Math.min(bytes - sent, temp.length);
				output.write(temp, 0, num);
				sent += num;
			}
			output.close();
		} catch (IOException e) {
			LOG.error(Thread.currentThread().toString()+" Error reading data, "+e.getMessage());
			e.printStackTrace();
		}

	}

	@Override
	public void newSessionIS(URI uri, InputStream input) {
		byte[] temp = new byte[JxioConnectionServer.msgPoolBuffSize];
		long bytes = getBytes(uri);
		LOG.info(Thread.currentThread().toString()+" going to read " + bytes + " bytes");
		long read = 0;
		try {
			int num;
			while ((num = input.read(temp)) != -1) {
				read += num;
			}
			input.close();
			if (read != bytes) {
				LOG.error("Number of bytes read " + read + " is different from number of bytes requested " + bytes);
			}
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