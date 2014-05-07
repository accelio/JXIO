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

package com.mellanox.jxio.tests.controlPathDuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.ArrayList;
import java.util.List;
import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.EventReason;
import com.mellanox.jxio.ServerPortal;
import com.mellanox.jxio.ServerSession;
import com.mellanox.jxio.Msg;
import java.net.URI;
import java.net.URISyntaxException;

public class CPDTestServer {

	private final EventQueueHandler           listen_eqh;
	private final TestServerCallbacks         tsc;
	private final ServerPortal                listener;
	private boolean                           multiThread;
	private int                               clientsNum;
	private URI                               uri = null;
	private final List<CPDServerPortalWorker> workers;
	private int                               currentIndex;
	private final static Log                  LOG = LogFactory.getLog(CPDTestServer.class.getCanonicalName());

	public CPDTestServer(String args[]) {
		this.parseCommandArgs(args);

		listen_eqh = new EventQueueHandler(null);
		tsc = new TestServerCallbacks();
		int numThreads = (multiThread) ? clientsNum : 0;
		this.workers = new ArrayList<CPDServerPortalWorker>();
		listener = new ServerPortal(listen_eqh, uri, tsc);
		for (int i = 0; i < numThreads; i++) {
			this.workers.add(new CPDServerPortalWorker(i, listener.getUriForServer()));
		}
	}

	public void run_test() {
		for (CPDServerPortalWorker spw : workers) {
			spw.start();
		}
		listen_eqh.run();
	}

	// callbacks for the listener server portal
	public class TestServerCallbacks implements ServerPortal.Callbacks {

		public void onSessionNew(ServerSession.SessionKey sesKey, String srcIP) {
			ServerSession session = new ServerSession(sesKey, new TestSessionCallbacks());

			if (multiThread) {
				// primitive round robin
				CPDServerPortalWorker spw = workers.get(currentIndex);
				currentIndex++;
				if (currentIndex == workers.size()) {
					currentIndex = 0;
				}
				listener.forward(spw.getPortal(), session);
			} else {
				listener.accept(session);
			}
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
		}
	}

	// callbacks for the listener server portal
	public class TestSessionCallbacks implements ServerSession.Callbacks {

		public void onRequest(Msg msg) {
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			if (session_event == EventName.SESSION_CLOSED) { // normal exit
				LOG.info("[EVENT] Got event SESSION_CLOSED");
			} else {
				LOG.error("");
			}
		}

		public boolean onMsgError(Msg msg, EventReason reason) {
			return true;
		}
	}

	private void parseCommandArgs(String[] args) {
		String server_ip = args[0];
		String server_port = args[1];
		multiThread = (Integer.parseInt(args[2]) == 1) ? true : false;
		clientsNum = Integer.parseInt(args[3]);
		String url_string = "rdma://" + server_ip + ":" + server_port;
		try {
			this.uri = new URI(url_string);
		} catch (URISyntaxException e) {
			LOG.error("Bad URI, Aborting test...");
			System.exit(1);
		}
	}

	// main
	public static void main(String[] args) {
		CPDTestServer test = new CPDTestServer(args);
		test.run_test();
	}
}
