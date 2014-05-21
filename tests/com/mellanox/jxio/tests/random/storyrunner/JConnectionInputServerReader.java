package com.mellanox.jxio.tests.random.storyrunner;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.jxioConnection.JxioConnection;
import com.mellanox.jxio.jxioConnection.JxioConnectionServer;

//import com.mellanox.jxio.jxioConnection.MultiBufOutputStream;

public class JConnectionInputServerReader implements JxioConnectionServer.Callbacks {

	private static final Log LOG  = LogFactory.getLog(JConnectionInputServerReader.class.getCanonicalName());
	private final byte[]     temp = new byte[JxioConnection.msgPoolBuffSize];
	private final long       rate;
	private final long       seed;

	JConnectionInputServerReader(long rate, long seed) {
		this.rate = rate;
		this.seed = seed;
	}

	public void newSessionStart(String uriString, OutputStream outStream) {
		LOG.debug(Thread.currentThread().toString()+" newSessionStart callback " + uriString);
		Random random = new Random(seed);
		try {
			URI uri = new URI(uriString);
			String query = uri.getQuery();
			try {
				long bytesToRead = Long.parseLong(query.split("size=")[1]);
				LOG.info(Thread.currentThread().toString()+" going to send " + bytesToRead + " bytes");
				long sent = 0;
				while (sent < bytesToRead) {
					Thread.sleep(rate);
					int randTimes = random.nextInt((int) Math
					        .ceil((double) (bytesToRead - sent) / JxioConnection.msgPoolBuffSize)) + 1;
					for (int i = 0; i < randTimes && sent < bytesToRead; i++) {
						outStream.write(temp, 0, (int) Math.min(bytesToRead - sent, (long) temp.length));
						sent += temp.length;
					}
				}
				outStream.close();
			} catch (IOException e) {
				LOG.error(Thread.currentThread().toString()+" Error reading data, reason:" + e.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
