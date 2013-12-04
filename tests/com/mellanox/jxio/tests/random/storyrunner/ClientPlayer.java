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
	private final long        msgDelayMicroSec;
	private WorkerThread      workerThread;
	private ClientSession     client;
	private MsgPool           mp;
	private boolean           isClosing = false;
	private int               counterReceivedMsgs;
	private int               counterSentMsgs;
	private final MsgPoolData poolData;

	public ClientPlayer(int id, URI uri, long startDelaySec, long runDurationSec, long msgRate, MsgPoolData pool) {
		this.name = new String("CP[" + id + "]");
		this.uri = uri;
		this.runDurationSec = runDurationSec;
		this.startDelaySec = startDelaySec;
		this.msgDelayMicroSec = (msgRate > 0) ? (1000000 / msgRate) : 0;
		this.poolData = pool;
		LOG.debug("new " + this.toString() + " done");
	}

	public String toString() {
		return name;
	}

	@Override
	public void attach(WorkerThread workerThread) {
		LOG.info(this.toString() + ": attaching to WorkerThread '" + workerThread.toString() + "'" + ", startDelay = "
		        + startDelaySec + "sec, runDuration = " + runDurationSec + "sec");

		this.workerThread = workerThread;
		this.mp = new MsgPool(poolData.getCount(), poolData.getInSize(), poolData.getOutSize());

		// register initialize timer
		TimerList.Timer tInitialize = new InitializeTimer(this, this.startDelaySec * 1000000);
		this.workerThread.start(tInitialize);
	}

	protected void sendMsgTimerStart() {
		if (this.msgDelayMicroSec > 0) {
			if (LOG.isTraceEnabled()) {
				LOG.trace(this.toString() + ": starting send timer for " + this.msgDelayMicroSec + "usec");
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

			// register next send timer
			if (!this.player.isClosing) {
				sendMsgTimerStart();
				// send msg
				Msg m = this.player.mp.getMsg();
				if (m != null) {
					String str = "Client " + this.toString() + " sending msg # " + counterSentMsgs;
					Utils.writeMsg(m, str);
					if (this.player.client.sendMessage(m)) {
						counterSentMsgs++;
					} else {
						LOG.error(this.toString() + " failed to send a message");
						m.returnToParentPool();
					}
				} else {// MsgPool is empty (client used up all the msgs and
					    // they didn't return from client yet
					if (LOG.isDebugEnabled()) {
						LOG.debug(this.toString() + "no more messages in pool: skipping a timer");
					}
				}

			}
		}
	}

	@Override
	protected void initialize() {
		LOG.debug(this.toString() + ": initializing");

		// add ClientPlayer's name to the connect URI request
		String struri = new String(this.uri.toString() + "?name=" + toString());
		URI connecturi = null;
		try {
			connecturi = new URI(struri);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		// connect to server
		LOG.info(this.toString() + ": connecting to '" + connecturi.toString() + "'");
		this.client = new ClientSession(this.workerThread.getEQH(), connecturi, new JXIOCallbacks(this));

		// register terminate timer
		TimerList.Timer tTerminate = new TerminateTimer(this, this.runDurationSec * 1000000);
		this.workerThread.start(tTerminate);
	}

	@Override
	protected void terminate() {
		LOG.info(this.toString() + ": terminating. sent " + this.counterSentMsgs + "msgs");
		this.isClosing = true;
		this.client.close();
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
			LOG.info(c.toString() + ": onSessionEstablished");
			this.c.sendMsgTimerStart();
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			if (this.c.isClosing == true && session_event == EventName.SESSION_TEARDOWN) {
				LOG.info(c.toString() + ": onSESSION_TEARDOWN, reason='" + reason.toString() + "'");
				if (c.counterReceivedMsgs != c.counterSentMsgs) {
					LOG.error(c.toString() + "there were " + c.counterSentMsgs + " sent and " + c.counterReceivedMsgs
					        + " received");
				} else {
					LOG.info(c.toString() + "sent and received same # of msgs");
				}
				c.mp.deleteMsgPool();
			} else {
				LOG.error(c.toString() + ": onSessionError: event='" + session_event.toString() + "', reason='"
				        + reason.toString() + "'");
				System.exit(1);
			}
		}

		public void onReply(Msg msg) {
			counterReceivedMsgs++;
			if (!Utils.checkIntegrity(msg)) {
				LOG.error(c.toString() + "checksums for message #" + counterReceivedMsgs + " do not match.");
				System.exit(1);
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace(c.toString() + ": onReply: msg = " + msg.toString() + "#" + counterReceivedMsgs);
			}
			msg.returnToParentPool();
		}
	}
}
