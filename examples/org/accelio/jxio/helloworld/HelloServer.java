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
package org.accelio.jxio.helloworld;

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

public class HelloServer {

	private final static Log        LOG = LogFactory.getLog(HelloServer.class.getCanonicalName());
	private final MsgPool           mp;
	private final ServerPortal      server;
	private final EventQueueHandler eqh;
	private ServerSession           session;

	HelloServer(URI uri) {
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

		URI uri = null;
		try {
			uri = new URI("rdma://" + serverhostname + ":" + port + "/");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}

		HelloServer server = new HelloServer(uri);
		server.run();
		
		LOG.info("Server is releasing JXIO resources and exiting");
		server.releaseResources();
	}

	public static void usage() {
		LOG.info("Usage: ./runHelloServer.sh <SERVER_IPADDR> [<PORT>]");
	}

	class MyPortalCallbacks implements ServerPortal.Callbacks {
		private final HelloServer server;

		MyPortalCallbacks(HelloServer server) {
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
		private final HelloServer server;

		MySessionCallbacks(HelloServer server) {
			this.server = server;
		}

		public void onRequest(Msg msg) {
			LOG.info("[SUCCESS] Got a message request! Prepare the champagne!");

			// Read message String
			byte ch;
			StringBuffer buffer = new StringBuffer();
			while (msg.getIn().hasRemaining() && ((ch = msg.getIn().get()) > -1)) {
	            buffer.append((char)ch);
	        }
			LOG.info("msg is: '" + buffer.toString() + "'");

			// Write response
			try {
				msg.getOut().put("Hello to you too, Client".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Just suppress the exception handling in this demo code
			}

			// Send the response
			try {
				this.server.session.sendResponse(msg);
			} catch (IOException e) {
				// all exceptions thrown extend IOException
				LOG.error(e.toString());
			}

			// Un-comment here if case you want to close the connection from Server side...
			// LOG.info("Closing the session...");
			// this.server.session.close();
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
