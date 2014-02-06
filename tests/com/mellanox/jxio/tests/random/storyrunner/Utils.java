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
package com.mellanox.jxio.tests.random.storyrunner;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.CRC32;

import com.mellanox.jxio.Msg;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class Utils {

	public final static int  HEADER_SIZE = 24;
	// 24=time(long)+checksum(long)+serialNumber(int)+size(int)
	private final static Log LOG         = LogFactory.getLog(Utils.class.getSimpleName());

	// private c-tor to avoid instantiation of the class
	private Utils() {
	}

	public static void writeMsg(Msg m, int position, long time, int msgSerialNum) {
		CRC32 checksum = new CRC32();
		byte bytes[] = new byte[position];
		m.getOut().get(bytes, 0, position);
		m.getOut().position(0);// get in the above line moves the position
		checksum.update(bytes, 0, position);
		long lngChecksum = checksum.getValue();
		m.getOut().putLong(time);
		m.getOut().putLong(lngChecksum);
		m.getOut().putInt(msgSerialNum);
		m.getOut().putInt(position);
		m.getOut().put(bytes);
	}

	public static boolean checkIntegrity(Msg msg, int expectedSerialNum) {
		@SuppressWarnings("unused")
		long sendTime = msg.getIn().getLong();
		long rcvCheckSum = msg.getIn().getLong();
		int rcvSerialNum = msg.getIn().getInt();
		if (rcvSerialNum != expectedSerialNum) {
			LOG.error("msgs were not received by order. expected " + expectedSerialNum + " and got " + rcvSerialNum);
			return false;
		}
		int size = msg.getIn().getInt();
		byte bytes[] = new byte[size];
		msg.getIn().get(bytes);
		CRC32 checksum = new CRC32();
		checksum.update(bytes, 0, bytes.length);
		long calcChecksum = checksum.getValue();
		if (calcChecksum != rcvCheckSum) {
			LOG.error("checksums for message #" + expectedSerialNum + " does not match");
			return false;
		}
		return true;
	}

	public static String getQuery(String strUri) {
		URI uri = null;
		try {
			uri = new URI(strUri.trim());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return uri.getQuery();
	}

	public static String[] getQueryPairs(String query) {
		if (query != null)
			return query.split("&");
		return new String[0];
	}

	public static String getQueryPairKey(String queryParam) {
		if (queryParam != null)
			return queryParam.split("=")[0];
		return new String();
	}

	public static String getQueryPairValue(String queryParam) {
		if (queryParam != null)
			return queryParam.split("=")[1];
		return new String();
	}

	public static int randIntInRange(Random rand, int min, int max) {
		int randNum = rand.nextInt((max - min) + 1) + min;
		return randNum;
	}
	
	/**
     * Retrive the current machine's Infiniband IPv4 address
     * 
     * @return An IPv4 address
     */
	public static String getIP() {
		String myIP = null;
		try {
			Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()) {
				NetworkInterface ni = nis.nextElement();
				if (ni.getDisplayName().contains("eth4")) {
					for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
						if (ia.getNetworkPrefixLength() == 8 || ia.getNetworkPrefixLength() == 16
						        || ia.getNetworkPrefixLength() == 24) {
							myIP = ia.getAddress().getHostAddress();
						}
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return myIP;
	}
}
