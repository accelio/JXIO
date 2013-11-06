/*
 * * Copyright (C) 2013 Mellanox Technologies** Licensed under the Apache License, Version 2.0 (the "License");* you may
 * not use this file except in compliance with the License.* You may obtain a copy of the License at:**
 * http://www.apache.org/licenses/LICENSE-2.0** Unless required by applicable law or agreed to in writing, software*
 * distributed under the License is distributed on an "AS IS" BASIS,* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,*
 * either express or implied. See the License for the specific language* governing permissions and limitations under the
 * License.*
 */

package com.mellanox.jxio.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.*;

public class MySesPortalTest {

	private final static Log LOG = LogFactory.getLog(MySesPortalTest.class.getCanonicalName());
	EventQueueHandler        eqh1, eqh2;
	ServerPortal             man, worker;

	MySesPortalTest(String url) {
		eqh1 = new EventQueueHandler();
		MyPortalManagerCallbacks c = new MyPortalManagerCallbacks();
		this.man = new ServerPortal(eqh1, url, c);

		Thread t = new Thread(eqh1);
		t.start();

		eqh2 = new EventQueueHandler();
		MsgPool msgPool = new MsgPool(1, 4, 4);
		eqh2.bindMsgPool(msgPool);
		this.worker = new ServerPortal(eqh2, man.getUrlForServer());
		Thread t2 = new Thread(eqh2);
		t2.start();
	}

	public static void main(String[] args) {
		String url = args[0];
		String port = args[1];
		String combined_url = "rdma://" + url + ":" + port;

		MySesPortalTest spt = new MySesPortalTest(combined_url);
	}

	public void close() {
		// serverManager.close();
	}

	class MyPortalManagerCallbacks implements ServerPortal.Callbacks {
		// ServerPortal man;

		public void onSessionNew(long ptrSes, String uriSrc, String srcIP) {
			MySesServerCallbacks c = new MySesServerCallbacks();
			ServerSession ses = new ServerSession(ptrSes, c);
			c.serverSession = ses;
			LOG.debug("****************************" + man);
			man.forward(worker, ses);
		}

		public void onSessionEvent(int session_event, String reason) {
			String event;
			switch (session_event) {
				case 0:
					event = "SESSION_REJECT";
					// ses.close(); // Added
					break;
				case 1:
					event = "SESSION_TEARDOWN";
					// ses.close(); // Added
					break;
				case 2:
					event = "CONNECTION_CLOSED";
					// ses.close(); // Added
					break;
				case 3:
					event = "CONNECTION_ERROR";
					// ses.close(); // Added
					break;
				case 4:
					event = "SESSION_ERROR";
					// ses.close(); // Added
					break;
				default:
					event = "UNKNOWN";
					// ses.close(); // Added
					break;
			}
			LOG.error("GOT EVENT " + event + "because of " + reason);
		}
	}
}
