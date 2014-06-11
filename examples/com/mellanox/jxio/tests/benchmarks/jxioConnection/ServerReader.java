package com.mellanox.jxio.tests.benchmarks.jxioConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.jxioConnection.JxioConnection;
import com.mellanox.jxio.jxioConnection.JxioConnectionServer;

//import com.mellanox.jxio.jxioConnection.MultiBufOutputStream;

public class ServerReader implements JxioConnectionServer.Callbacks {

	private static final Log LOG  = LogFactory.getLog(ServerReader.class.getCanonicalName());
	private final byte[]     temp = new byte[JxioConnection.msgPoolBuffSize];

	public void newSessionStart(String uriString, OutputStream outStream) {

		try {
			URI uri = new URI(uriString);
			String query = uri.getQuery();

			try {
				long bytesToRead = Long.parseLong(query.split("size=")[1]);
				LOG.info("going to send " + bytesToRead + " bytes");
				// outStream.skip(bytesToRead);
				long sent = 0;;
				while (sent < bytesToRead) {
					outStream.write(temp);
					sent += temp.length;
				}
				outStream.close();
			} catch (IOException e) {
				LOG.error("Error reading data, reason:" + e.getMessage());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
