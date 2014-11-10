/*
 ** Copyright (C) 2013 Mellanox Technologies
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at:
 **
 ** http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 ** either express or implied. See the License for the specific language
 ** governing permissions and  limitations under the License.
 **
 */
package org.accelio.jxio.tests.benchmarks.jxioConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.accelio.jxio.jxioConnection.Constants;
import org.accelio.jxio.jxioConnection.JxioConnection;
import org.accelio.jxio.jxioConnection.JxioConnectionServer;
import org.accelio.jxio.jxioConnection.impl.JxioResourceManager;

public class StreamClient extends Thread {

	private static final Log LOG  = LogFactory.getLog(StreamClient.class.getCanonicalName());
	private final long       bytes;
	private final URI        uri;
	private final byte[]     temp = new byte[Constants.MSGPOOL_BUF_SIZE];
	private final String     name;
	private JxioConnection   connection;
	private String           type;
	private int              repeats;

	public StreamClient(URI uri, String type, int index, int repeats) {
		this.uri = uri;
		this.type = type;
		name = "[" + type + " StreamClient_" + index + "]";
		bytes = Long.parseLong(uri.getQuery().split("size=")[1]);
		this.repeats = repeats;
	}

	public void run() {
		for (int i = 0; i < repeats; i++) {
			try {
				long time = System.nanoTime();
				connection = new JxioConnection(uri);
				if (type.compareTo("input") == 0)
					read();
				else if (type.compareTo("output") == 0)
					write();
				else
					throw new UnsupportedOperationException("stream type " + type + " is not supported");
				calcBW(time);
				connection.disconnect();
			} catch (ConnectException e1) {
				e1.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public void read() throws IOException {
		LOG.info(this.toString() + " going to read " + bytes);
		InputStream input = connection.getInputStream();
		long read = 0;
		int n = 0;
		while ((n = input.read(temp)) != -1) {
			read += n;
		}
		if (read != bytes) {
			LOG.error("Number of bytes read " + read + " is different from number of bytes requested " + bytes);
		}
		input.close();
	}

	public void write() throws IOException {
		LOG.info(this.toString() + " going to write " + bytes);
		OutputStream output = connection.getOutputStream();
		long sent = 0;
		int n = 0;
		while (sent < bytes) {
			n = (int) Math.min(bytes - sent, temp.length);
			output.write(temp, 0, n);
			sent += n;
		}
		output.close();
	}

	public void calcBW(long t) {
		long time = System.nanoTime() - t;
		LOG.info(this.toString() + " Time to transfer data in nano: " + time);
		float gigas = (float) bytes / 1000000000;
		float milli = (float) time / 1000000;
		LOG.info(this.toString() + " [----------------  BW[MB/s]: " + (gigas / milli * 1000000) + " ----------------]");
	}

	// rdma://$2:$3/data?stream=input&size=$6"
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String uriString;
		URI uri;
		try {
			ExecutorService es = Executors.newFixedThreadPool(Integer.parseInt(args[3]) + Integer.parseInt(args[4]));
			// input
			for (int i = 0; i < Integer.parseInt(args[3]); i++) {
				uriString = String.format("rdma://%s:%s/data?size=%s", args[0], args[1], args[2]);
				uri = new URI(uriString);
				es.submit(new StreamClient(uri, "input", i, Integer.parseInt(args[5])));
			}
			// output
			for (int i = 0; i < Integer.parseInt(args[4]); i++) {
				uriString = String.format("rdma://%s:%s/data?size=%s", args[0], args[1], args[2]);
				uri = new URI(uriString);
				es.submit(new StreamClient(uri, "output", i, Integer.parseInt(args[5])));
			}
			es.shutdown();
			es.awaitTermination(5, TimeUnit.MINUTES);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String toString() {
		return this.name;
	}
}
