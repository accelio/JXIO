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
package org.accelio.jxio.simpletest;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.accelio.jxio.Msg;
import org.accelio.jxio.MsgPool;
import org.accelio.jxio.EventName;
import org.accelio.jxio.EventReason;
import org.accelio.jxio.ServerPortal;
import org.accelio.jxio.ServerSession;
import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.WorkerCache.Worker;

public class SimpleServer {

	private final static Log        LOG = LogFactory.getLog(SimpleServer.class.getCanonicalName());
	private final MsgPool           mp;
	private final ServerPortal      server;
	private final EventQueueHandler eqh;
	private ServerSession           session;

	public static long numberofRsps = 0;
	public static long numberofReqs = 0;
	public static long maxNumberofRsps = 600000;
	public static long PRINT_COUNTER = 10000;

	SimpleServer(URI uri) {
		this.eqh = new EventQueueHandler(null);
		this.mp = new MsgPool(256, 100, 100);
		eqh.bindMsgPool(mp);
		this.server = new ServerPortal(eqh, uri, new MyPortalCallbacks(this));
	}

	public void run() {
		LOG.info("waiting for JXIO incoming connections");
		eqh.run();
	}

	public void releaseResources() {
		eqh.releaseMsgPool(mp);
		mp.deleteMsgPool();
		eqh.close();
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
			return;
		}

		final String serverhostname = args[0];
		final int port = Integer.parseInt(args[1]);
		if (args.length > 2)
			maxNumberofRsps = Long.parseLong(args[2]);
		if (args.length > 3)
			PRINT_COUNTER = Long.parseLong(args[3]);

		URI uri = null;
		try {
			uri = new URI("rdma://" + serverhostname + ":" + port + "/");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}

		SimpleServer server = new SimpleServer(uri);
		server.run();

		LOG.info("Server is releasing JXIO resources and exiting");
		server.releaseResources();
	}

	public static void usage() {
		LOG.info("Usage: ./runSimpleServer.sh <SERVER_IPADDR> <PORT> [PRINT_COUNTER]");
	}

	class MyPortalCallbacks implements ServerPortal.Callbacks {
		private final SimpleServer server;

		MyPortalCallbacks(SimpleServer server) {
			this.server = server;
		}

		public void onSessionNew(ServerSession.SessionKey sesID, String srcIP, Worker hint) {
			LOG.info("[SUCCESS] Got event onSessionNew from " + srcIP + ", URI='" + sesID.getUri() + "'");
			this.server.session = new ServerSession(sesID, new MySessionCallbacks(server));
			this.server.server.accept(session);
		}

		public void onSessionEvent(EventName event, EventReason reason) {
			LOG.info("[EVENT] Got event " + event + " because of " + reason);
		}
	}

	class MySessionCallbacks implements ServerSession.Callbacks {
		private final SimpleServer server;

		MySessionCallbacks(SimpleServer server) {
			this.server = server;
		}

		public void onRequest(Msg msg) {
			++numberofReqs;

			if (numberofReqs % PRINT_COUNTER == 0){
				// Read message String
				byte ch;
				StringBuffer buffer = new StringBuffer();
				while (msg.getIn().hasRemaining() && ((ch = msg.getIn().get()) > -1)) {
					buffer.append((char)ch);
				}
				LOG.info("Got message request " + numberofReqs + " : '" + buffer.toString() + "'");
			}

			// Write response
			try {
				msg.getOut().put("Simple response".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Just suppress the exception handling in this demo code
			}

			// Send the response
			try {
				++numberofRsps;
				LOG.trace("sending rsp " + numberofRsps);
				this.server.session.sendResponse(msg);
			} catch (IOException e) {
				// all exceptions thrown extend IOException
				LOG.error(e.toString());
			}

			// Un-comment here if case you want to close the connection from Server side...
			// if (numberofRsps == maxNumberofRsps){
			//	LOG.info("Closing the session...");
			//	this.server.session.close();
			// }
		}

		public void onSessionEvent(EventName event, EventReason reason) {
			String str = "[EVENT] Got event " + event + " because of " + reason;
			if (event == EventName.SESSION_CLOSED) { // normal exit
				LOG.info(str);
			} else {
				LOG.error(str);
			}
			// Comment here if case you don't want to exit the server, stop the EQH
			LOG.info("Stopping the main EQH loop...");
			eqh.stop();
		}

		public boolean onMsgError(Msg msg, EventReason reason) {
			LOG.info("[ERROR] onMsgErrorCallback. reason=" + reason);
			return true;
		}
	}
}
