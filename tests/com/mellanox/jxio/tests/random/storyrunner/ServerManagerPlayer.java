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

import com.mellanox.jxio.ServerManager;

public class ServerManagerPlayer extends GeneralPlayer {

	private final static Log LOG = LogFactory.getLog(ServerManagerPlayer.class.getSimpleName());

	private final URI        uri;
	private final long       durationSec;
	@SuppressWarnings("unused")
    private final long       startDelaySec;
	private WorkerThread     workerThread;
	private WorkerThreads    workerThreads;
	private ServerManager    listener;

	public ServerManagerPlayer(URI uri, long startDelaySec, long durationSec, WorkerThreads workerThreads) {
		this.uri = uri;
		this.durationSec = durationSec;
		this.startDelaySec = startDelaySec;
		this.workerThreads = workerThreads;
		LOG.debug("new " + this.toString() + " done");
	}

	public String toString() {
		return "ServerManagerPlayer (uri=" + uri.toString() + ")";
	}

	@Override
	public void attach(WorkerThread workerThread) {
		LOG.info(this.toString() + " attaching to WorkerThread (" + workerThread.toString() + ")");
		this.workerThread = workerThread;

		// connect to server
		this.listener = new ServerManager(this.workerThread.getEQH(), uri.toString(), new JXIOCallbacks(this));

		// register terminate timer
		TimerList.Timer tTerminate = new TerminatTimer(this, this.durationSec * 1000000);
		this.workerThread.start(tTerminate);
	}

	String getUriForServer() {
		return this.listener.getUriForServer();
	}

	@Override
	protected void close() {
		LOG.info("closing");
		this.listener.close();
		LOG.info("exiting - SUCCESS (???)");
		System.exit(0);
	}

	public void notifyReadyforWork(ServerSessionPlayer ss, long newSessionKey) {
		CompleteOnNewSessionForward action = new CompleteOnNewSessionForward(this, ss, newSessionKey);
		this.workerThread.addWorkAction(action);
	}

	public class CompleteOnNewSessionForward implements WorkerThread.QueueAction {
		private final ServerManagerPlayer sm;
		private final ServerSessionPlayer ss;
		private final long                newSessionKey;

		public CompleteOnNewSessionForward(ServerManagerPlayer sm, ServerSessionPlayer ss, long newSessionKey) {
			this.sm = sm;
			this.ss = ss;
			this.newSessionKey = newSessionKey;
		}

		public void doAction(WorkerThread workerThread) {
			this.sm.listener.forward(this.ss.getServerSession(), this.newSessionKey);
		}
	}

	class JXIOCallbacks implements ServerManager.Callbacks {
		private final ServerManagerPlayer sm;

		public JXIOCallbacks(ServerManagerPlayer sm) {
			this.sm = sm;
		}

		public void onSession(long newSessionKey, String uriSrc, String srcIP) {
			LOG.info("onSessionNew: uri=" + uriSrc + ", srcaddr=" + srcIP);
			ServerSessionPlayer ss = new ServerSessionPlayer(sm, newSessionKey, srcIP);
			WorkerThread worker = workerThreads.getWorkerThread();
			worker.addWorkAction(ss.getAttachAction());
		}

		public void onSessionEvent(int session_event, String reason) {
			LOG.error("onSessionError: event='" + session_event + "', reason='" + reason + "'");
			System.exit(1);
		}
	}
}
