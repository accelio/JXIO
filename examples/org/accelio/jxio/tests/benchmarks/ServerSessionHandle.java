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
package org.accelio.jxio.tests.benchmarks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.Msg;
import org.accelio.jxio.EventName;
import org.accelio.jxio.EventReason;
import org.accelio.jxio.ServerSession;
import org.accelio.jxio.exceptions.JxioGeneralException;
import org.accelio.jxio.exceptions.JxioSessionClosedException;
import org.accelio.jxio.tests.benchmarks.ServerPortalWorker;

public class ServerSessionHandle {

	// the server session
	private final ServerSession ss;

	// the thread that will run the server portal
	private final ServerPortalWorker spw;

	// logger
	private final static Log LOG = LogFactory.getLog(ServerSessionHandle.class.getCanonicalName());

	// cTor
	public ServerSessionHandle(ServerSession.SessionKey sesKey, ServerPortalWorker s) {
		ss = new ServerSession(sesKey, new SessionServerCallbacks());
		spw = s;
	}

	public ServerSession getSession() {
		return ss;
	}

	// session callbacks
	public class SessionServerCallbacks implements ServerSession.Callbacks {

		public void onRequest(Msg msg) {
			// answer back with the same message that was received
			msg.getOut().position(msg.getOut().capacity()); // simulate 'out_msgSize' was written into buffer
			try {
				ss.sendResponse(msg);
			} catch (JxioSessionClosedException e) {
				LOG.error("request was not handled. session already closed!");
			} catch (JxioGeneralException e) {
				LOG.error("request was not handled: " + e.toString());
			}
		}

		public void onSessionEvent(EventName event, EventReason reason) {
			LOG.debug("got event " + event.toString() + ", the reason is " + reason.toString());
			System.out.println("got event " + event.toString() + ", the reason is " + reason.toString());
			if (event == EventName.SESSION_CLOSED) {
				spw.sessionClosed();
			}
		}

		public boolean onMsgError(Msg msg, EventReason reason) {
			if (ServerSessionHandle.this.ss.getIsClosing()) {
				LOG.debug("On Message Error while closing. Reason is=" + reason);
			} else {
				LOG.error("On Message Error. Reason is=" + reason);
			}
			return true;
		}
	}
}
