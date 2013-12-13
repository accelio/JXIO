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

import com.mellanox.jxio.Msg;

import java.util.zip.CRC32;

public final class Utils {

	// private c-tor to avoid instantiation of the class
	private Utils() {
	}

	public static void writeMsg(Msg m, String str, long time) {
		CRC32 checksum = new CRC32();
		int size = str.length();
		byte bytes[] = str.getBytes();
		checksum.update(bytes, 0, size);
		long lngChecksum = checksum.getValue();
		m.getOut().putLong(time);
		m.getOut().putLong(lngChecksum);
		m.getOut().putInt(size);
		m.getOut().put(bytes);
	}

	public static boolean checkIntegrity(Msg msg) {
		@SuppressWarnings("unused")
		long sendTime = msg.getIn().getLong();
		long rcvCheckSum = msg.getIn().getLong();
		int size = msg.getIn().getInt();
		byte bytes[] = new byte[size];
		msg.getIn().get(bytes);
		CRC32 checksum = new CRC32();
		checksum.update(bytes, 0, bytes.length);
		long calcChecksum = checksum.getValue();
		if (calcChecksum != rcvCheckSum) {
			return false;
		}
		return true;
	}
}
