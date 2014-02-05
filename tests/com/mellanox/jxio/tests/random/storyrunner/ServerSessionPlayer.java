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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;
import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventReason;
import com.mellanox.jxio.ServerSession;
import com.mellanox.jxio.ClientSession;

public class ServerSessionPlayer {

	private final static Log         LOG          = LogFactory.getLog(ServerSessionPlayer.class.getSimpleName());

	private static int               id           = 0;
	private final String             name;
	private final long               sk;
	private final ServerPortalPlayer spp;
	private final ServerSession      server;
	private String                   nextHop      = new String();
	private String                   nextHopQuery = new String();
	private ClientSession            nextHopClient;
	private MsgPool                  nextHopMP;
	private int                      counterReceivedMsgs;
	private int                      counterSentMsgs;
	private Random                   random;

	public ServerSessionPlayer(ServerPortalPlayer spp, ServerSession.SessionKey sesKey, String srcIP, long seed) {
		this.name = "SSP[" + id++ + "]";
		this.spp = spp;
		this.sk = sesKey.getSessionPtr();
		prepareForNextHop(sesKey.getUri());
		this.server = new ServerSession(sesKey, new JXIOServerCallbacks());
		this.random = new Random(seed);
		LOG.debug("new " + this.toString() + " done");
	}

	public ServerPortalPlayer getServerPortalPlayer() {
		return spp;
	}

	public String toString() {
		return name;
	}

	protected ServerSession getServerSession() {
		return server;
	}

	private void prepareForNextHop(String uri) {
		int mpcount = 0;
		int msginsize = 0;
		int msgoutsize = 0;
		// build and prepare nextHop strings
		String query = Utils.getQuery(uri);
		String[] queryParams = Utils.getQueryPairs(query);
		for (String param : queryParams) {
			if (this.nextHop.isEmpty() && Utils.getQueryPairKey(param).equals("nextHop")) {
				this.nextHop = Utils.getQueryPairValue(param);
			} else {
				if (Utils.getQueryPairKey(param).equals("name")) {
					String origName = Utils.getQueryPairValue(param);
					param = "name=" + this + ":" + origName;
				}
				if (Utils.getQueryPairKey(param).equals("mpcount")) {
					mpcount = Integer.valueOf(Utils.getQueryPairValue(param));
					mpcount += 16; // to overcome accelio's internal server batching
				}
				if (Utils.getQueryPairKey(param).equals("msginsize")) {
					msginsize = Integer.valueOf(Utils.getQueryPairValue(param));
				}
				if (Utils.getQueryPairKey(param).equals("msgoutsize")) {
					msgoutsize = Integer.valueOf(Utils.getQueryPairValue(param));
				}
				this.nextHopQuery += nextHopQuery.isEmpty() ? "?" : "&";
				this.nextHopQuery += param;
			}
		}

		if (!this.nextHop.isEmpty() && mpcount > 0 && (msgoutsize + msginsize) > 0) {
			this.nextHopMP = new MsgPool(mpcount, msginsize, msgoutsize);
			LOG.info(this.toString() + ": new MsgPool: " + this.nextHopMP);
		}
	}

	class JXIOServerCallbacks implements ServerSession.Callbacks {
		private final ServerSessionPlayer outer = ServerSessionPlayer.this;

		public void onRequest(Msg msg) {
			if (LOG.isDebugEnabled())
				LOG.debug(outer.toString() + ": onRequest(" + msg + ")");

			if (!Utils.checkIntegrity(msg, outer.counterReceivedMsgs)) {
				LOG.error(outer.toString() + "FAILURE: Message did not arrive ok!");
				System.exit(1);
			}

			if (LOG.isTraceEnabled()) {
				LOG.trace(outer.toString() + ": onRequest: msg (#" + outer.counterReceivedMsgs + ") = " + msg);
			}

			if (outer.nextHop.isEmpty()) {
				if (LOG.isDebugEnabled())
					LOG.debug(outer.toString() + ": sendResponse(" + msg + ")");
				int position = Utils.randIntInRange(random, 0, msg.getOut().limit() - Utils.HEADER_SIZE);
				Utils.writeMsg(msg, position, 0, outer.counterSentMsgs);
				outer.counterSentMsgs++;
				outer.counterReceivedMsgs++;
				outer.server.sendResponse(msg);
			} else {
				// server session is in proxy mode (nextHopClient), check if client need to be connected
				if (outer.nextHopClient == null) {
					outer.nextHopClient = prepareNextHopClient();
				}

				// send mirror msg to next hoop server
				Msg nextHopMsg = prepareNextHopMsg(msg);
				if (LOG.isDebugEnabled())
					LOG.debug(outer.toString() + ": sendMessage(" + nextHopMsg + ")");
				outer.nextHopClient.sendRequest(nextHopMsg);
			}

		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			switch (session_event){
				case SESSION_CLOSED:
					LOG.info(outer.toString() + ": SESSION_CLOSED. reason='" + reason + "'");
					LOG.info(outer.toString() + ": received " + counterReceivedMsgs + " msgs");
					if (!outer.nextHop.isEmpty()) {
						LOG.info(outer.toString() + ": closing nextHopClient");
						outer.nextHopClient.close();
					}
					break;
				default:
					LOG.error(outer.toString() + " got " + session_event.toString() + " reason='" + reason + "'");
					break;
			}
		}

		public boolean onMsgError(Msg msg, EventReason reason) {
			if (outer.server.getIsClosing()){
				LOG.debug(outer.toString() + ": MsgError in msg " + msg.toString() + " reason='" + reason + "'");
			}else{
				LOG.error(outer.toString() + ": MsgError in msg " + msg.toString() + " reason='" + reason + "'");
			}
			return true;
		}

		private ClientSession prepareNextHopClient() {
			URI connectUri = null;
			try {
				connectUri = new URI("rdma://" + outer.nextHop + "/" + outer.nextHopQuery);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			LOG.info(outer.toString() + ": connecting proxy to '" + connectUri + "'");
			return new ClientSession(outer.spp.getWorkerThread().getEQH(), connectUri, new JXIOProxyCallbacks());
		}

		private Msg prepareNextHopMsg(Msg msg) {
			Msg nextHopMsg = outer.nextHopMP.getMsg();
			if (nextHopMsg != null) {
				msg.getIn().position(0);
				nextHopMsg.getOut().put(msg.getIn());
				nextHopMsg.setUserContext(msg);
			}
			return nextHopMsg;
		}
	}

	class JXIOProxyCallbacks implements ClientSession.Callbacks {
		private final ServerSessionPlayer outer = ServerSessionPlayer.this;

		public void onMsgError(Msg msg, EventReason reason) {
			LOG.info(outer.toString() + ": MsgError in msg " + msg.toString() + " reason='" + reason + "'");
			msg.returnToParentPool();
		}

		public void onSessionEstablished() {
			LOG.debug(outer.toString() + ": onSessionEstablished");
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			switch (session_event) {
				case SESSION_CLOSED:
				case SESSION_REJECT:
					LOG.debug(outer.toString() + ": on" + session_event.toString() + ", reason='" + reason + "'");
					break;
				default:
			LOG.error(outer.toString() + ": FAILURE: onSessionError: event='" + session_event + "', reason='" + reason
			        + "'");
			System.exit(1); // Failure in test - eject!
					break;
			}
			
		}

		public void onReply(Msg msg) {
			if (LOG.isDebugEnabled())
				LOG.debug(outer.toString() + ": onReply(" + msg + ")");
			Msg returnHopMsg = (Msg) msg.getUserContext();
			returnHopMsg.getOut().put(msg.getIn());
			if (LOG.isDebugEnabled())
				LOG.debug(outer.toString() + ": sendResponse(" + returnHopMsg + ")");
			outer.server.sendResponse(returnHopMsg);
			msg.returnToParentPool();
		}
	}
}
