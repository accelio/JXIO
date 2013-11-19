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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;
import com.mellanox.jxio.ServerPortal;
import com.mellanox.jxio.ServerSession;
import com.mellanox.jxio.EventName;

public class ServerSessionPlayer extends GeneralPlayer {

	private final static Log          LOG = LogFactory.getLog(ServerSessionPlayer.class.getSimpleName());

	private final String              srcIP;
	private final long                sk;
	private final ServerPortalPlayer  sp;
	private WorkerThread              workerThread;
	private ServerSession             server;
	private MsgPool                   mp;

	public ServerSessionPlayer(ServerPortalPlayer sp, long newSessionKey, String srcIP) {
		this.sp = sp;
		this.sk = newSessionKey;
		this.srcIP = srcIP;
		
		this.server = new ServerSession(sk, new JXIOServerCallbacks(this));
		LOG.debug("new " + this.toString() + " done");
	}
	
	public ServerPortalPlayer getServerPortalPlayer(){ return sp;}

	public String toString() {
		return "ServerSessionPlayer (srcaddr=" + srcIP + ")";
	}

	
	@Override
	public void attach(WorkerThread workerThread) {
		LOG.info(this.toString() + " attaching to WorkerThread (" + workerThread.toString() + ")");
		this.workerThread = workerThread;

		// prepare MsgPool
	//	this.mp = new MsgPool(10, 64 * 1024, 256);
	//	this.workerThread.getEQH().bindMsgPool(this.mp);

		// update ServerManager that it can 'accept' this 'newSessionKey'
		this.sp.notifyReadyforWork(this, this.sk);
	}

	@Override
	protected void close() {
		LOG.info("closing (TODO)");
	}

	protected ServerSession getServerSession() {
		return server;
	}

	class JXIOServerCallbacks implements ServerSession.Callbacks {
		private final ServerSessionPlayer ss;

		public JXIOServerCallbacks(ServerSessionPlayer ss) {
			this.ss = ss;
		}

		public void onRequest(Msg msg) {
			LOG.info("onRequest: msg = " + msg.toString());
			ss.server.sendResponce(msg);
		}

        public void onSessionEvent(EventName session_event, String reason) {
			LOG.error("onSessionError: event='" + session_event.toString() + "', reason='" + reason + "'");
			System.exit(1);
		}

		public void onMsgError() {
			LOG.info("onMsgError");
		}
	}
}
