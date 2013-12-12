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

package com.mellanox.jxio.tests.benchmarks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.ClientSession;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;
import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventReason;

public class DataPathTestClient implements Runnable {

	EventQueueHandler        eqh;
	ClientSession            cs;
	MsgPool                  msgPool;

	double                   totalBTW   = 0;
	int                      totalCnt   = 0;
	int                      maxCnt     = 20;
	int                      totalPPS   = 0;
	FileWriter               fstream;
	BufferedWriter           out;
	String                   filePath;
	boolean                  closed     = false;
	boolean                  quit       = true;

	static boolean           lat_test   = false;
	static int               num_of_pkts;
	static int               max_to = -1;

	int                      in_msgSize;
	int                      out_msgSize;
	int                      msgSize;
	long                     cnt, print_cnt;
	boolean                  firstTime;
	long                     startTime;
	private final static Log LOG        = LogFactory.getLog(DataPathTestClient.class.getCanonicalName());

	public DataPathTestClient(String uriString, int msg_size, String file_path, String test_type) {

		eqh = new EventQueueHandler();
		filePath = file_path;

		if (test_type.equals("w")) {
			in_msgSize = 0;
			out_msgSize = msg_size;
			num_of_pkts = 50;
		} else if (test_type.equals("r")) {
			in_msgSize = msg_size;
			out_msgSize = 0;
			num_of_pkts = 50;
		} else if (test_type.equals("l")) {
			lat_test = true;
			in_msgSize = msg_size;
			out_msgSize = msg_size;
			num_of_pkts = 1;
			max_to = 0;
		} else {
			System.out.println("invalid option, Aborting");
			max_to = 0;
		}

		URI uri = null;
		try {
			uri = new URI(uriString);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cs = new ClientSession(eqh, uri, new DataPathClientCallbacks());
		msgPool = new MsgPool(1024, in_msgSize, out_msgSize);
		msgSize = (in_msgSize > out_msgSize) ? in_msgSize : out_msgSize;
		int msgKSize = msgSize / 1024;
		if (msgKSize == 0) {
			print_cnt = 4000000;
		} else {
			print_cnt = 4000000 / msgKSize;
			// System.out.println("print_cnt = " + print_cnt);
		}
		cnt = 0;
		firstTime = true;
	}

	public void run() {
		try {
			// Create file
			fstream = new FileWriter(filePath, true);
			out = new BufferedWriter(fstream);
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

		for (int i = 0; i < num_of_pkts; i++) {
			Msg msg = msgPool.getMsg();
			if (msg == null) {
				print("Cannot get new message");
				break;
			}
			msg.getOut().position(msg.getOut().capacity()); // simulate 'out_msgSize' was writen into buffer
			if (!cs.sendMessage(msg)) {
				print("Error sending");
				msgPool.releaseMsg(msg);
			}
		}

		if (lat_test) {
			while (quit) {
				eqh.runEventLoop(1, max_to);
			}
		} else {
			eqh.runEventLoop(-1, max_to);
		}
		msgPool.deleteMsgPool();
		eqh.close();
	}

	public void print(String str) {
		LOG.debug("********" + str);
	}

	public static void main(String[] args) {

		String url = "rdma://" + args[0] + ":" + Integer.parseInt(args[1]);
		Runnable test = new DataPathTestClient(url, Integer.parseInt(args[3]), args[4], args[2]);
		test.run();
	}

	class DataPathClientCallbacks implements ClientSession.Callbacks {

		public void onMsgError() {
			// logger.log(Level.debug, "onMsgErrorCallback");
			print("onMsgErrorCallback");
		}

		public void onSessionEstablished() {
			// logger.log(Level.debug, "[SUCCESS] Session Established");
			print("Session Established");
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			print("GOT EVENT " + session_event.toString() + " because of " + reason.toString());
			eqh.stop();
		}

		public void onReply(Msg msg) {
			print("Got Reply");

			if (lat_test) {
				if (firstTime) {
					startTime = System.nanoTime();
					firstTime = false;
					msg.getOut().position(msg.getOut().capacity()); // simulate 'out_msgSize' was writen into buffer
					cs.sendMessage(msg);
					return;
				} else {
					long time = System.nanoTime() - startTime;
					System.out.println("latency = " + time / 1000 + " micro" + " msg_size = " + in_msgSize);
					try {
						out.write(time / 1000 + "," + in_msgSize + "\n");
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					quit = false;
					cs.close();
					return;
				}
			}

			if (firstTime) {
				startTime = System.nanoTime();
				firstTime = false;
			}

			cnt++;
			if (cnt == print_cnt) {
				long delta = System.nanoTime() - startTime;
				long pps = (cnt * 1000000000) / delta;
				double bw = (1.0 * pps * msgSize / (1024 * 1024));
				// System.out.println("delta = " + delta + " cnt = " + cnt);
				System.out.println("TPS = " + pps + " BW = " + bw + " msg_size = " + msgSize);

				totalBTW += bw;
				totalPPS += pps;

				if (++totalCnt == maxCnt) {
					try {
						out.write(msg.getIn().capacity() + "," + msg.getOut().capacity() + "," + totalPPS / maxCnt
						        + "," + totalBTW / maxCnt + "\n");
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					cs.close();
					closed = true;
				}
				cnt = 0;
				startTime = System.nanoTime();
			}
			if (!closed) {
				msg.getOut().position(msg.getOut().capacity()); // simulate 'out_msgSize' was writen into buffer
				if (!cs.sendMessage(msg)) {
					msgPool.releaseMsg(msg);
				}
			}
		}
	}
}
