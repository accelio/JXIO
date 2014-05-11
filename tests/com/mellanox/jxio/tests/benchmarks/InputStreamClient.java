package com.mellanox.jxio.tests.benchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.jxioConnection.JxioConnection;

public class InputStreamClient extends Thread {

	private static final Log LOG  = LogFactory.getLog(InputStreamClient.class.getCanonicalName());
	private final long       bytesToRead;
	private final URI        uri;
	private final int        msgpoolCount;
	private final byte[]     temp = new byte[JxioConnection.msgPoolBuffSize];
	private final String     name;

	public InputStreamClient(URI uri, int msgpoolCount) {
		this.msgpoolCount = msgpoolCount;
		this.uri = uri;
		name = "[InputStreamClient]";
		bytesToRead = Long.parseLong(uri.getQuery().split("size=")[1]);
	}

	public void run() {
		JxioConnection connection;
		try {
			connection = new JxioConnection(uri, msgpoolCount);
			InputStream input = connection.getInputStream();
			Long time = System.nanoTime();
			input.skip(bytesToRead);
			/*
			 * long sent = 0;
			 * while (sent < bytesToRead) {
			 * sent += input.read(temp);
			 * // System.out.println("client: "+temp[0]+" "+temp[65535]);
			 * }
			 */
			time = System.nanoTime() - time;
			LOG.info(this.toString() + " Time to transfer data in nano: " + time);
			float gigas = (float) bytesToRead / 1000000000;
			float milli = (float) time / 1000000;
			LOG.info(this.toString() + " [----------------  BW[MB/s]: " + (gigas / milli * 1000000)
			        + " ----------------]");
			connection.disconnect();
		} catch (ConnectException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		URI uri;
		try {
			for (int i = 0; i < Integer.parseInt(args[2]); i++) {
				uri = new URI(/* "rdma://r-sw-fatty12-ib:1111/data?size="+bytesToRead */args[0]);
				InputStreamClient client = new InputStreamClient(uri, Integer.parseInt(args[1]));
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
