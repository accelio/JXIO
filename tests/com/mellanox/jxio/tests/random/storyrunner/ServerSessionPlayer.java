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
import com.mellanox.jxio.ServerSession;

public class ServerSessionPlayer extends GeneralPlayer {

	private final static Log          LOG = LogFactory.getLog(ServerSessionPlayer.class.getSimpleName());

	private final String              srcIP;
	private final long                sk;
	private final ServerManagerPlayer sm;
	private WorkerThread              workerThread;
	private ServerSession             server;
	private MsgPool                   mp;

	public ServerSessionPlayer(ServerManagerPlayer sm, long newSessionKey, String srcIP) {
		this.sm = sm;
		this.sk = newSessionKey;
		this.srcIP = srcIP;
		LOG.debug("new " + this.toString() + " done");
	}

	public String toString() {
		return "ServerSessionPlayer (srcaddr=" + srcIP + ")";
	}

	@Override
	public void attach(WorkerThread workerThread) {
		String uri = this.sm.getUrlForServer();
		LOG.info(this.toString() + " attaching to WorkerThread (" + workerThread.toString() + ")");
		this.workerThread = workerThread;

		// connect to server
		this.server = new ServerSession(this.workerThread.getEQH(), uri, new JXIOCallbacks(this));

		// prepare MsgPool
		this.mp = new MsgPool(10, 64 * 1024, 256);
		this.workerThread.getEQH().bindMsgPool(this.mp);

		// update ServerManager that it can 'accept' this 'newSessionKey'
		this.sm.notifyReadyforWork(this, this.sk);
	}

	@Override
	protected void close() {
		LOG.info("closing (TODO)");
	}

	protected ServerSession getServerSession() {
		return server;
	}

	class JXIOCallbacks implements ServerSession.Callbacks {
		private final ServerSessionPlayer ss;

		public JXIOCallbacks(ServerSessionPlayer ss) {
			this.ss = ss;
		}

		public void onRequest(Msg msg) {
			LOG.info("onRequest: msg = " + msg.toString());
			ss.server.sendResponce(msg);
		}

		public void onSessionError(int session_event, String reason) {
			LOG.error("onSessionError: event='" + session_event + "', reason='" + reason + "'");
			System.exit(1);
		}

		public void onMsgError() {
			LOG.info("onMsgError");
		}
	}
}
