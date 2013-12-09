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
package com.mellanox.jxio.helloworld;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.*;

public class HelloServer {

	private final static Log        LOG = LogFactory.getLog(HelloServer.class.getCanonicalName());
	private final MsgPool           mp;
	private final ServerPortal      server;
	private final EventQueueHandler eqh;
	private ServerSession           session;

	HelloServer(URI uri) {
		this.eqh = new EventQueueHandler();
		this.mp = new MsgPool(256, 100, 100);
		eqh.bindMsgPool(mp);
		this.server = new ServerPortal(eqh, uri, new MyPortalCallbacks(this));
	}

	public void run() {
		LOG.info("waiting for JXIO incoming connections");
		eqh.run();
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
	}

	public static void usage() {
		LOG.info("Usage: ./runHelloServer.sh <SERVER_IPADDR> [<PORT>]");
	}

	class MyPortalCallbacks implements ServerPortal.Callbacks {
		private final HelloServer server;

		MyPortalCallbacks(HelloServer server) {
			this.server = server;
		}

		public void onSessionNew(long sessionKey, String uriSrc, String srcIP) {
			LOG.info("[SUCCESS] Got event onSessionNew from " + srcIP + ", URI='" + uriSrc + "'");
			this.server.session = new ServerSession(sessionKey, new MySessionCallbacks(server));
			this.server.server.accept(session);
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			LOG.info("[EVENT] Got event " + session_event + " because of " + reason);
		}
	}

	class MySessionCallbacks implements ServerSession.Callbacks {
		private final HelloServer server;

		MySessionCallbacks(HelloServer server) {
			this.server = server;
		}

		public void onRequest(Msg msg) {
			LOG.info("[SUCCESS] Got a message request! Prepare the champagne!");
			LOG.info("msg is: '" + msg + "'");
			this.server.session.sendResponce(msg);
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			LOG.info("[EVENT] Got event " + session_event + " because of " + reason);
		}

		public void onMsgError() {
			LOG.info("[ERROR] onMsgErrorCallback");
		}
	}
}
