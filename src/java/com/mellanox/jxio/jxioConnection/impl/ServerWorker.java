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
package com.mellanox.jxio.jxioConnection.impl;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.EventReason;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;
import com.mellanox.jxio.ServerPortal;
import com.mellanox.jxio.ServerSession;
import com.mellanox.jxio.ServerSession.SessionKey;
import com.mellanox.jxio.jxioConnection.JxioConnectionServer;

public class ServerWorker extends Thread implements BufferManager {

	private final static Log                     LOG            = LogFactory.getLog(ServerWorker.class
	                                                                    .getCanonicalName());
	private final ServerPortal                   sp;
	private final EventQueueHandler              eqh;
	public final int                             portalIndex;
	private final SessionServerCallbacks         callbacks;
	private final JxioConnectionServer.Callbacks appCallbacks;
	private final String                         name;
	private ArrayList<MsgPool>                   msgPools;
	private boolean                              sessionClosed  = false;
	private boolean                              waitingToClose = false;
	private Msg                                  msg            = null;
	private int                                  count          = 0;
	private ServerSession                        session        = null;
	private MultiBufOutputStream                 output;
	private Thread                               worker;
	private boolean                              stop           = false;
	private String                               uri            = null;
	private boolean                              firstMsg       = true;

	// cTor
	public ServerWorker(int index, int inMsgSize, int outMsgSize, URI uri, int numMsgs,
	        JxioConnectionServer.Callbacks appCallbacks) {
		portalIndex = index;
		name = "[ServerWorker " + portalIndex + " ]";
		eqh = new EventQueueHandler(new EqhCallbacks(numMsgs, inMsgSize, outMsgSize));
		this.msgPools = new ArrayList<MsgPool>();
		MsgPool pool = new MsgPool(numMsgs, inMsgSize, outMsgSize);
		msgPools.add(pool);
		eqh.bindMsgPool(pool);
		sp = new ServerPortal(eqh, uri);
		callbacks = new SessionServerCallbacks();
		this.appCallbacks = appCallbacks;

		LOG.info(this.toString() + " is up and waiting for requests");
	}

	public void run() {
		worker = Thread.currentThread();
		while (!stop) {
			LOG.info("waiting for a new connection");
			eqh.runEventLoop(1, -1); // to get the forward going
			appCallbacks.newSessionStart(uri, output);
			close();
		}
	}

	public void prepareSession(ServerSession ss, SessionKey sk) {
		session = ss;
		sessionClosed = false;
		waitingToClose = false;
		msg = null;
		count = 0;
		output = new MultiBufOutputStream(this);
		uri = sk.getUri();
		firstMsg = true;
	}

	public ServerPortal getPortal() {
		return sp;
	}

	public SessionServerCallbacks getSessionCallbacks() {
		return callbacks;
	}

	public void sessionClosed() {
		LOG.info(this.toString() + " disconnected from a Session");
		session = null;
		JxioConnectionServer.updateWorkers(this);
	}

	public EventQueueHandler getEqh() {
		return eqh;
	}

	public ByteBuffer getNextBuffer() throws IOException {
		if (!firstMsg || msg == null) {
			if (msg != null) {
				count++;
				session.sendResponse(msg);
				msg = null;
			}
			do {
				eqh.runEventLoop(1, -1);
			} while (!sessionClosed && msg == null);
			if (sessionClosed) {
				throw new IOException("Session was closed, no buffer avaliable");
			}
		}
		firstMsg = false;
		return msg.getOut();
	}

	public void flush() {
		if (msg != null) {
			count++;
			session.sendResponse(msg);
			msg = null;
		}
	}

	public void close() {
		if (waitingToClose || session == null)
			return;
		LOG.info(this.toString() + " closing session sent " + count + " msgs");
		waitingToClose = true;
		session.close();
		while (!sessionClosed) {
			eqh.runEventLoop(-1, -1);
		}
		sessionClosed();
	}

	// session callbacks
	public class SessionServerCallbacks implements ServerSession.Callbacks {
		public void onRequest(Msg m) {
			msg = m;
			if (waitingToClose) {
				session.discardRequest(m);
			}
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			LOG.info(ServerWorker.this.toString() + " got event " + session_event.toString() + ", the reason is "
			        + reason.toString());
			if (session_event == EventName.SESSION_CLOSED) {
				sessionClosed = true;
				waitingToClose = true;
				eqh.breakEventLoop();
			}
		}

		public boolean onMsgError(Msg msg, EventReason reason) {
			LOG.error(this.toString() + " onMsgErrorCallback. reason is " + reason);
			return true;
		}
	}

	class EqhCallbacks implements EventQueueHandler.Callbacks {
		private final ServerWorker outer = ServerWorker.this;
		private final int          numMsgs;
		private final int          inMsgSize;
		private final int          outMsgSize;

		public EqhCallbacks(int msgs, int in, int out) {
			numMsgs = msgs;
			inMsgSize = in;
			outMsgSize = out;
		}

		public MsgPool getAdditionalMsgPool(int in, int out) {
			MsgPool mp = new MsgPool(numMsgs, inMsgSize, outMsgSize);
			LOG.warn(this.toString() + " " + outer.toString() + ": new MsgPool: " + mp);
			outer.msgPools.add(mp);
			return mp;
		}

	}

	public String toString() {
		return this.name;
	}
}