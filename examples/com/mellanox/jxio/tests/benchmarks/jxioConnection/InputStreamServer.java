package com.mellanox.jxio.tests.benchmarks.jxioConnection;

import java.net.URI;
import java.net.URISyntaxException;

import com.mellanox.jxio.jxioConnection.JxioConnectionServer;

public class InputStreamServer {

	public static void main(String[] args) {

		try {
			String uriString = String.format("rdma://%s:%s", args[0], args[1]);
			URI uri = null;
			try {
				uri = new URI(uriString);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			JxioConnectionServer conServer = new JxioConnectionServer(uri, Integer.parseInt(args[3]),
			        new ServerReader(), Integer.parseInt(args[2]));
			conServer.start();
		} catch (Throwable t) {
			t.printStackTrace();
			t.getCause().printStackTrace();
		}
	}
}
