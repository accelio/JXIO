package com.mellanox.jxio.tests.benchmarks.jxioConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.jxioConnection.JxioConnection;
import com.mellanox.jxio.jxioConnection.JxioConnectionServer;

public class StreamClient extends Thread {

	private static final Log LOG  = LogFactory.getLog(StreamClient.class.getCanonicalName());
	private final long       bytes;
	private final URI        uri;
	private final byte[]     temp = new byte[JxioConnectionServer.msgPoolBuffSize];
	private final String     name;
	private JxioConnection   connection;
	private String           type;

	public StreamClient(URI uri, String type, int index) {
		this.uri = uri;
		this.type = type;
		name = "[" + type + " StreamClient_"+index+"]";
		bytes = Long.parseLong(uri.getQuery().split("size=")[1]);
	}

	public void run() {
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
	}

	public void read() throws IOException {
		LOG.info(this.toString() + " going to read " + bytes);
		InputStream input = connection.getInputStream();
		long sent = 0;
		while (sent < bytes) {
			sent += input.read(temp);
		}
		input.close();
	}

	public void write() throws IOException {
		LOG.info(this.toString() + " going to write " + bytes);
		OutputStream output = connection.getOutputStream();
		long sent = 0;
		while (sent < bytes) {
			output.write(temp);
			sent += temp.length;
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
			// input
			for (int i = 0; i < Integer.parseInt(args[3]); i++) {
				uriString = String.format("rdma://%s:%s/data?stream=input&size=%s", args[0], args[1], args[2]);
				uri = new URI(uriString);
				StreamClient client = new StreamClient(uri, "input", i);
				client.start();
			}
			// output
			for (int i = 0; i < Integer.parseInt(args[4]); i++) {
				uriString = String.format("rdma://%s:%s/data?stream=output&size=%s", args[0], args[1], args[2]);
				uri = new URI(uriString);
				StreamClient client = new StreamClient(uri, "output", i);
				client.start();
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String toString() {
		return this.name;
	}

}
