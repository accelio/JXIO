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
import java.util.LinkedList;
import java.util.Queue;
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
	private final int                msgBatchSize;
	private final long               msgDelayMicroSec;
	private Queue<Msg>               queueDelayedMsgs;
	private boolean                  isClosing    = false;
	private Random                   random;
	private CallbacksCounter         callbacksCounter;

	public ServerSessionPlayer(ServerPortalPlayer spp, ServerSession.SessionKey sesKey, String srcIP, int msgRate,
	        int msgBatch, CallbacksCounter callbacksCounter, long seed) {
		this.name = "SSP[" + id++ + "]";
		this.spp = spp;
		this.sk = sesKey.getSessionPtr();
		this.queueDelayedMsgs = new LinkedList<Msg>();
		this.msgDelayMicroSec = (msgRate > 0) ? (1000000 / msgRate) : 0;
		this.msgBatchSize = msgBatch;
		prepareForNextHop(sesKey.getUri());
		this.server = new ServerSession(sesKey, new JXIOServerCallbacks());
		this.callbacksCounter = callbacksCounter;
		this.random = new Random(seed);
		sendMsgTimerStart();
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
		String useMsgMirror = new String();

		// build and prepare nextHop strings
		String query = Utils.getQuery(uri);
		String[] queryParams = Utils.getQueryPairs(query);
		for (String param : queryParams) {
			if (this.nextHop.isEmpty() && Utils.getQueryPairKey(param).equals("nextHop")) {
				this.nextHop = Utils.getQueryPairValue(param);
				continue;
			}

			if (useMsgMirror.isEmpty() && Utils.getQueryPairKey(param).equals("useMsgMirror")) {
				useMsgMirror = Utils.getQueryPairValue(param);
				continue;
			}

			// else prepare nextHop URI
			if (Utils.getQueryPairKey(param).equals("name")) {
				String origName = Utils.getQueryPairValue(param);
				param = "name=" + this + ":" + origName;
				continue;
			}
			if (Utils.getQueryPairKey(param).equals("mpcount")) {
				mpcount = Integer.valueOf(Utils.getQueryPairValue(param));
				mpcount += 16; // to overcome accelio's internal server batching
				continue;
			}
			if (Utils.getQueryPairKey(param).equals("msginsize")) {
				msginsize = Integer.valueOf(Utils.getQueryPairValue(param));
				continue;
			}
			if (Utils.getQueryPairKey(param).equals("msgoutsize")) {
				msgoutsize = Integer.valueOf(Utils.getQueryPairValue(param));
				continue;
			}
			this.nextHopQuery += nextHopQuery.isEmpty() ? "?" : "&";
			this.nextHopQuery += param;
		}

		if (!this.nextHop.isEmpty() && !useMsgMirror.equals("1") && mpcount > 0 && (msgoutsize + msginsize) > 0) {
			this.nextHopMP = new MsgPool(mpcount, msginsize, msgoutsize);
			LOG.info(this.toString() + ": new nextHop MsgPool: " + this.nextHopMP);
		}
	}

	protected void sendMsgTimerStart() {
		if (this.msgDelayMicroSec > 0) {
			if (LOG.isTraceEnabled()) {
				LOG.trace(this.toString() + ": starting send timer for " + this.msgDelayMicroSec + " usec");
			}
			TimerList.Timer tSendMsg = new SendMsgTimer(this.msgDelayMicroSec);
			this.spp.workerThread.start(tSendMsg);
		}
	}

	private void sendMsg(Msg msg) {
		int position = Utils.randIntInRange(this.random, 0, msg.getOut().limit() - Utils.HEADER_SIZE);
		Utils.writeMsg(msg, position, 0, counterSentMsgs);
		if (LOG.isTraceEnabled())
			LOG.trace(this.toString() + ": sendResponse(" + msg + ")");
		if (server.sendResponse(msg) == false) {
			LOG.error(this.toString() + ": FAILURE: sendResponse with error on msg=" + msg);
			System.exit(1);
		}
		this.counterSentMsgs++;
	}

	private class SendMsgTimer extends TimerList.Timer {
		private final ServerSessionPlayer outer = ServerSessionPlayer.this;

		public SendMsgTimer(long durationMicroSec) {
			super(durationMicroSec);
		}

		@Override
		public void onTimeOut() {
			if (LOG.isTraceEnabled()) {
				LOG.trace("SendMsgTimer: " + outer.toString());
			}

			if (outer.isClosing) {
				return;
			}
			// register next send timer
			sendMsgTimerStart();
			// send msgs
			int numMsgToSend = outer.msgBatchSize;
			while (numMsgToSend > 0) {
				numMsgToSend--;
				Msg msg = outer.queueDelayedMsgs.poll();
				if (msg == null) {
					// MsgQueue is empty
					if (LOG.isTraceEnabled()) {
						LOG.trace(outer.toString() + ": no more messages in queue: skipping a timer");
					}
					return;
				}
				outer.sendMsg(msg);
			}
		}
	}

	class JXIOServerCallbacks implements ServerSession.Callbacks {
		private final ServerSessionPlayer outer = ServerSessionPlayer.this;

		public void onRequest(Msg msg) {
			if (LOG.isTraceEnabled())
				LOG.trace(outer.toString() + ": onRequest: msg (#" + outer.counterReceivedMsgs + ") = " + msg);

			if (!Utils.checkIntegrity(msg, outer.counterReceivedMsgs)) {
				LOG.error(outer.toString() + ": FAILURE: Message #" + outer.counterReceivedMsgs + " did not arrive ok!");
				System.exit(1);
			}

			outer.counterReceivedMsgs++;

			if (outer.nextHop.isEmpty()) {
				if (msgDelayMicroSec > 0) {
					outer.queueDelayedMsgs.add(msg);
				} else {
					outer.sendMsg(msg);
				}
			} else {
				// server session is in proxy mode (nextHopClient), check if client need to be connected
				if (outer.nextHopClient == null)
					outer.nextHopClient = prepareNextHopClient();

				// send mirror msg to next hoop server
				Msg nextHopMsg = prepareNextHopMsg(msg);
				if (LOG.isTraceEnabled())
					LOG.trace(outer.toString() + ": sendRequest(" + nextHopMsg + ")");
				if (outer.nextHopClient.sendRequest(nextHopMsg) == false) {
					LOG.error(outer.toString() + ": FAILURE: sendRequest to nextHopClient with msg=" + nextHopMsg);
					System.exit(1);
				}
				outer.counterSentMsgs++;
			}
			
			callbacksCounter.possiblyThrowExecption("[onRequest] Callback exception in onRequest in ServerSessionPlayer.");
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			switch (session_event) {
				case SESSION_CLOSED:
					LOG.info(outer.toString() + ": SESSION_CLOSED. reason='" + reason + "'");
					LOG.info(outer.toString() + ": received " + counterReceivedMsgs + " msgs");
					if (!outer.nextHop.isEmpty()) {
						LOG.info(outer.toString() + ": closing nextHopClient");
						outer.nextHopClient.close();
					}
					outer.isClosing = true;
					break;
				default:
					LOG.error(outer.toString() + " got " + session_event.toString() + " reason='" + reason + "'");
					break;
			}
						
			callbacksCounter.possiblyThrowExecption("[onSessionEvent] Callback exception in onSessionEvent in ServerSessionPlayer.");
		}

		public boolean onMsgError(Msg msg, EventReason reason) {
			if (outer.server.getIsClosing()) {
				LOG.debug(outer.toString() + ": MsgError in msg " + msg.toString() + " reason='" + reason + "'");
			} else {
				LOG.error(outer.toString() + ": MsgError in msg " + msg.toString() + " reason='" + reason + "'");
			}
			
			callbacksCounter.possiblyThrowExecption("[onMsgError] Callback exception in onMsgError in ServerSessionPlayer.");
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
			if (outer.nextHopMP == null) {
				// Use MsgMirror
				return msg.getMirror(true);
			}

			// Use Msg copy instead of MsgMirror
			Msg nextHopMsg = outer.nextHopMP.getMsg();
			if (nextHopMsg != null) {
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
			callbacksCounter.possiblyThrowExecption("[onMsgError] Callback exception in onMsgError in ServerSessionPlayer.");
		}

		public void onSessionEstablished() {
			LOG.debug(outer.toString() + ": onSessionEstablished");
			callbacksCounter.possiblyThrowExecption("[onSessionEstablished] Callback exception in onSessionEstablished in ServerSessionPlayer.");
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			switch (session_event) {
				case SESSION_CLOSED:
				case SESSION_REJECT:
					LOG.debug(outer.toString() + ": on" + session_event.toString() + ", reason='" + reason + "'");
					break;
				default:
					LOG.error(outer.toString() + ": FAILURE: onSessionError: event='" + session_event + "', reason='" + reason + "'");
					System.exit(1); // Failure in test - eject!
					break;
			}

			callbacksCounter.possiblyThrowExecption("[onSessionEvent] Callback exception in onSessionEvent in ServerSessionPlayer.");
		}

		public void onResponse(Msg msg) {
			Msg returnHopMsg;
			if (outer.nextHopMP == null) {
				returnHopMsg = msg.getMirror(true);
			} else {
				returnHopMsg = (Msg) msg.getUserContext();
				returnHopMsg.getOut().put(msg.getIn());
			}
			if (LOG.isTraceEnabled())
				LOG.trace(outer.toString() + ": onResponse(" + msg + "), " + "sendResponse(" + returnHopMsg + ")");

			if (outer.server.sendResponse(returnHopMsg) == false) {
				LOG.error(outer.toString() + ": FAILURE: sendResponse with error on msg=" + returnHopMsg);
			}
			if (outer.nextHopMP != null) {
				msg.returnToParentPool();
			}

			callbacksCounter.possiblyThrowExecption("[onResponse] Callback exception in onResponse in ServerSessionPlayer.");
		}
	}
}
