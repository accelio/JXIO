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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;
import com.mellanox.jxio.ClientSession;

public class ClientPlayer extends GeneralPlayer {

	private final static Log LOG = LogFactory.getLog(ClientPlayer.class.getSimpleName());

	private final URI        uri;
	private final long       durationSec;
	private final long       startDelaySec;
	private final long       msgDelayMicroSec;
	private WorkerThread     workerThread;
	private ClientSession    client;
	private MsgPool          mp;
	private boolean          isClosing = false;

	public ClientPlayer(URI uri, long startDelaySec, long durationSec, long msgRate) {
		this.uri = uri;
		this.durationSec = durationSec;
		this.startDelaySec = startDelaySec;
		this.msgDelayMicroSec = (msgRate > 0) ? (1000000 / msgRate) : 0;
		LOG.debug("new " + this.toString() + " done");
	}

	public String toString() {
		return "ClientPlayer (uri=" + uri.toString() + ")";
	}

	@Override
	public void attach(WorkerThread workerThread) {
		LOG.info(this.toString() + " attaching to WorkerThread (" + workerThread.toString() + ")");
		this.workerThread = workerThread;

		LOG.debug(this.toString() + " sleeping for " + this.startDelaySec/1000 + " msec ,,,");
		try {
	        Thread.sleep(this.startDelaySec * 1000);
        } catch (InterruptedException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

		LOG.debug(this.toString() + " connecting,,,");
		this.client = new ClientSession(this.workerThread.getEQH(), uri.toString(), new JXIOCallbacks(this));

		// prepare MsgPool
		this.mp = new MsgPool(10, 64 * 1024, 256);

		// register terminate timer
		TimerList.Timer tTerminate = new TerminatTimer(this, this.durationSec * 1000000);
		this.workerThread.start(tTerminate);
	}

	protected void sendMsgTimerStart() {
    	if (this.msgDelayMicroSec > 0) {
    		LOG.info(this.toString() + " starting send timer for " + this.msgDelayMicroSec + "usec");
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
			LOG.info("SendMsgTimer: " + this.player.toString());

			// register next send timer
			if (!this.player.isClosing) {
				sendMsgTimerStart();

				// send msg
				Msg m = this.player.mp.getMsg();
				if (this.player.client.sendMessage(m) != true)
					m.returnToParentPool();
			}
		}
	}

	@Override
	protected void close() {
		LOG.info("closing");
		this.isClosing = true;
		this.client.close();
	}

	class JXIOCallbacks implements ClientSession.Callbacks {
		private final ClientPlayer c;
		
		public JXIOCallbacks(ClientPlayer c) {
			this.c = c;
        }
		
		public void onMsgError() {
			LOG.info("onMsgErrorCallback");
		}

		public void onSessionEstablished() {
			LOG.info("onSessionEstablished");

			this.c.sendMsgTimerStart();
		}

		public void onSessionEvent(int session_event, String reason) {
			if (this.c.isClosing == true && session_event==1) {
				LOG.info("onSessionEvent: event='" + session_event + "', reason='" + reason + "'");
			} else {
				LOG.error("onSessionError: event='" + session_event + "', reason='" + reason + "'");
				System.exit(1);
			}
		}

		public void onReply(Msg msg) {
			LOG.info("onReply " + msg.toString());
			msg.returnToParentPool();
		}
	}
}
