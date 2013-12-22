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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;
import com.mellanox.jxio.ClientSession;
import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventReason;

public class ClientPlayer extends GeneralPlayer {

	private final static Log  LOG       = LogFactory.getLog(ClientPlayer.class.getSimpleName());

	private final String      name;
	private final URI         uri;
	private final long        runDurationSec;
	private final long        startDelaySec;
	private final int         msgBatchSize;
	private final long        msgDelayMicroSec;
	private final MsgPoolData poolData;
	private WorkerThread      workerThread;
	private ClientSession     client;
	private MsgPool           mp;
	private long              startSessionTime;
	private int               counterEstablished; // count connect responses: established or rejected
	private int               counterSentMsgs;
	private int               counterReceivedMsgs;
	private boolean           isClosing = false;

	public ClientPlayer(int id, URI uri, long startDelaySec, long runDurationSec, MsgPoolData pool, int msgRate, int msgBatch) {
		this.name = new String("CP[" + id + "]");
		this.uri = uri;
		this.runDurationSec = runDurationSec;
		this.startDelaySec = startDelaySec;
		this.msgDelayMicroSec = (msgRate > 0) ? (1000000 / msgRate) : 0;
		this.poolData = pool;
		this.msgBatchSize = msgBatch;
		LOG.debug("new " + this.toString() + " done");
	}

	public String toString() {
		return name;
	}

	@Override
	public void attach(WorkerThread workerThread) {
		LOG.info(this.toString() + ": attaching to WorkerThread '" + workerThread.toString() + "'" + ", startDelay="
		        + startDelaySec + "sec, runDuration=" + runDurationSec + "sec, msgRate=" + 1000000/msgDelayMicroSec + "pps");

		this.workerThread = workerThread;
		this.mp = new MsgPool(poolData.getCount(), poolData.getInSize(), poolData.getOutSize());

		// register initialize timer
		TimerList.Timer tInitialize = new InitializeTimer(this, this.startDelaySec * 1000000);
		this.workerThread.start(tInitialize);
	}

	protected void sendMsgTimerStart() {
		if (this.msgDelayMicroSec > 0) {
			if (LOG.isTraceEnabled()) {
				LOG.trace(this.toString() + ": starting send timer for " + this.msgDelayMicroSec + " usec");
			}
			TimerList.Timer tSendMsg = new SendMsgTimer(this.msgDelayMicroSec, this);
			this.workerThread.start(tSendMsg);
		}
	}

	private class SendMsgTimer extends TimerList.Timer {
		private final ClientPlayer player;

		public SendMsgTimer(long durationMicroSec, ClientPlayer player) {
			super(durationMicroSec);
			this.player = player;
		}

		@Override
		public void onTimeOut() {
			if (LOG.isTraceEnabled()) {
				LOG.trace("SendMsgTimer: " + this.player.toString());
			}

			if (this.player.isClosing) {
				return;
			}

			// register next send timer
			sendMsgTimerStart();

			// check if we have for full batch of buffers ready for sending
			if (this.player.mp.count() < this.player.msgBatchSize) {
				return;
			}

			// send msgs
			int numMsgToSend = this.player.msgBatchSize;
			while (numMsgToSend > 0) {
				numMsgToSend--;
				Msg m = this.player.mp.getMsg();
				if (m == null) {
					// MsgPool is empty (client used up all the msgs and
					// or they didn't return from server yet
					if (LOG.isDebugEnabled()) {
						LOG.debug(this.toString() + ": no more messages in pool: skipping a timer");
					}
					return;
				}
				String str = "Client " + this.toString() + " sending msg # " + this.player.counterSentMsgs;
				final long sendTime = System.nanoTime();
				Utils.writeMsg(m, str, sendTime);
				if (this.player.client.sendMessage(m)) {
					this.player.counterSentMsgs++;
				} else {
					LOG.error(this.toString() + " failed to send a message = " + m);
					m.returnToParentPool();
				}
			}
		}
	}

	@Override
	protected void initialize() {
		LOG.debug(this.toString() + ": initializing");

		// add ClientPlayer's name to the connect URI request
		String struri = this.uri.toString();
		struri += (this.uri.getQuery() == null) ? "?" : "&";
		struri += "name=" + toString();
		URI connecturi = null;
		try {
			connecturi = new URI(struri);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		// connect to server
		LOG.info(this.toString() + ": connecting to '" + connecturi.toString() + "'");
		this.startSessionTime = System.nanoTime();
		this.client = new ClientSession(this.workerThread.getEQH(), connecturi, new JXIOCallbacks(this));

		// register terminate timer
		TimerList.Timer tTerminate = new TerminateTimer(this, this.runDurationSec * 1000000);
		this.workerThread.start(tTerminate);
	}

	@Override
	protected void terminate() {
		LOG.info(this.toString() + ": terminating. sent " + this.counterSentMsgs + " msgs");
		if (!this.isClosing)
			this.client.close();
		this.isClosing = true;
		if (this.counterEstablished != 1) {
			LOG.error(this.toString() + ": FAILURE: session did not get established/rejected as expected");
			System.exit(1); // Failure in test - eject!			
		}
		if (this.mp.count() + (this.counterSentMsgs - this.counterReceivedMsgs) != this.mp.capacity()) {
			LOG.error(this.toString() + ": FAILURE: not all Msgs returned to MSgPoll: " + mp);
			System.exit(1); // Failure in test - eject!
		}
		this.mp.deleteMsgPool();
	}

	class JXIOCallbacks implements ClientSession.Callbacks {
		private final ClientPlayer c;

		public JXIOCallbacks(ClientPlayer c) {
			this.c = c;
		}

		public void onMsgError() {
			LOG.info(c.toString() + ": onMsgErrorCallback");
		}

		public void onSessionEstablished() {
			this.c.counterEstablished++;
			final long timeSessionEstablished = System.nanoTime() - c.startSessionTime;
			if (timeSessionEstablished > 100000000) { // 100 msec
				LOG.error(c.toString() + ": FAILURE: session establish took " + timeSessionEstablished / 1000 + " usec");
				System.exit(1); // Failure in test - eject!
			}
			LOG.info(c.toString() + ": onSessionEstablished. took " + timeSessionEstablished / 1000 + " usec");
			this.c.sendMsgTimerStart();
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			switch (session_event) {
				case SESSION_TEARDOWN:
					if (this.c.isClosing == true) {
						LOG.info(c.toString() + ": onSESSION_TEARDOWN, reason='" + reason.toString() + "'");
						if (c.counterReceivedMsgs != c.counterSentMsgs) {
							LOG.error(c.toString() + ": there were " + c.counterSentMsgs + " sent and "
							        + c.counterReceivedMsgs + " received");
						} else {
							LOG.info(c.toString() + ": SUCCESSFULLY received all sent msgs (" + c.counterReceivedMsgs + ")");
						}
						return;
					}
					break;
				case SESSION_REJECT:
					if (uri.getQuery().contains("reject=1")) {
						// Reject test completed SUCCESSFULLY
						LOG.info(c.toString() + ": SUCCESSFULLY got rejected as expected");
						this.c.counterEstablished++;
						this.c.isClosing = true;
						return;
					}
					break;
				default:
					break;
			}
			LOG.error(c.toString() + ": FAILURE: onSessionError: event='" + session_event.toString() + "', reason='" + reason.toString() + "'");
			System.exit(1); // Failure in test - eject!
		}

		public void onReply(Msg msg) {
			counterReceivedMsgs++;
			if (!Utils.checkIntegrity(msg)) {
				LOG.error(c.toString() + ": FAILURE: checksums for message #" + counterReceivedMsgs + " does not match");
				System.exit(1); // Failure in test - eject!
			}

			final long roundTrip = roundTrip(msg);
			if (roundTrip > 100000000) { // 100 milisec
				LOG.error(c.toString() + ": FAILURE: msg round trip took " + roundTrip / 1000 + " usec");
				System.exit(1); // Failure in test - eject!
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace(c.toString() + ": onReply: msg = " + msg.toString() + "#" + counterReceivedMsgs);
			}
			msg.returnToParentPool();
		}

		private long roundTrip(Msg m) {
			final long recTime = System.nanoTime();
			final long sendTime = m.getOut().getLong(0);
			final long rTrip = recTime - sendTime;
			if (LOG.isTraceEnabled()) {
				LOG.trace(c.toString() + ": roundTrip for message " + counterReceivedMsgs + " took " + rTrip / 1000
				        + " usec");
			}
			return rTrip;
		}
	}
}
