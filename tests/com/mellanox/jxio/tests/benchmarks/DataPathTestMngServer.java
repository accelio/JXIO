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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.*;

public class DataPathTestMngServer implements Runnable {

	private final static Log LOG      = LogFactory.getLog(DataPathTestMngServer.class.getCanonicalName());
	EventQueueHandler        eqh1, eqh2;
	ServerPortal             man, worker;
	int                      out_msgSize;
	int                      in_msgSize;

	static boolean           invalid  = false;
	static boolean           lat_test = false;

	public DataPathTestMngServer(URI uri, String test_type, int msg_size) {

		eqh1 = new EventQueueHandler();
		SesManagerCallbacks c = new SesManagerCallbacks();
		this.man = new ServerPortal(eqh1, uri, c);

		eqh2 = new EventQueueHandler();
		this.worker = new ServerPortal(eqh2, man.getUriForServer());

		if (test_type.equals("w")) {
			in_msgSize = msg_size;
			out_msgSize = 0;
		} else if (test_type.equals("r")) {
			in_msgSize = 0;
			out_msgSize = msg_size;
		} else if (test_type.equals("l")) {
			in_msgSize = msg_size;
			out_msgSize = msg_size;
			lat_test = true;
		} else {
			System.out.println("invalid option, Aborting");
			invalid = true;
		}
	}

	public void close() {
		man.close();
	}

	public void run1() {
		Thread t = new Thread(eqh1);
		t.start();
		Thread t2 = new Thread(eqh2);
		t2.start();
	}

	public void run() {
		if (!invalid) {
			Thread t = new Thread(eqh1);
			t.start();
			if (lat_test) {
				while (true) {
					eqh2.runEventLoop(-1, 0);
				}
			} else {
				Thread t2 = new Thread(eqh2);
				t2.start();
			}
		}
	}

	public void print(String str) {
		LOG.debug("********" + str);
	}

	public static void main(String[] args) {

		String uriString = "rdma://" + args[0] + ":" + Integer.parseInt(args[1]);
		URI uri = null;
		try {
			uri = new URI(uriString);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Runnable test = new DataPathTestMngServer(uri, args[2], Integer.parseInt(args[3]));
		test.run();
	}

	class SesManagerCallbacks implements ServerPortal.Callbacks {

		public void onSessionNew(long ptrSes, String uriSrc, String srcIP) {

			MsgPool msgPool = new MsgPool(1024, in_msgSize, out_msgSize);
			eqh2.bindMsgPool(msgPool);
			DataPathTestServer server = new DataPathTestServer(ptrSes);

			man.forward(worker, server.session);
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			LOG.error("GOT EVENT " + session_event.toString() + "because of " + reason.toString());
		}
	}
}
