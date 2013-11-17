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

import com.mellanox.jxio.ServerPortal;
import com.mellanox.jxio.EventName;

public class ServerPortalPlayer extends GeneralPlayer {

	private final static Log LOG = LogFactory.getLog(ServerPortalPlayer.class.getSimpleName());

	private final URI        uri;
	private final long       durationSec;
	@SuppressWarnings("unused")
    private final long       startDelaySec;
	private WorkerThread     workerThread;
	private WorkerThreads    workerThreads;
	private ServerPortal    listener;

	public ServerPortalPlayer(URI uri, long startDelaySec, long durationSec, WorkerThreads workerThreads) {
		this.uri = uri;
		this.durationSec = durationSec;
		this.startDelaySec = startDelaySec;
		this.workerThreads = workerThreads;
		LOG.debug("new " + this.toString() + " done");
		workerThreads.addPortal(this);
	}

	public String toString() {
		return "ServerPortalPlayer (uri=" + uri.toString() + ")";
	}

	@Override
	public void attach(WorkerThread workerThread) {
		LOG.info(this.toString() + " attaching to WorkerThread (" + workerThread.toString() + ")");
		this.workerThread = workerThread;

		// connect to server
		this.listener = new ServerPortal(this.workerThread.getEQH(), uri, new JXIOPortalCallbacks(this));

		// register terminate timer
		TimerList.Timer tTerminate = new TerminatTimer(this, this.durationSec * 1000000);
		this.workerThread.start(tTerminate);
	}

	URI getUriForServer() {
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
		private final ServerPortalPlayer sm;
		private final ServerSessionPlayer ss;
		private final long                newSessionKey;

		public CompleteOnNewSessionForward(ServerPortalPlayer sm, ServerSessionPlayer ss, long newSessionKey) {
			this.sm = sm;
			this.ss = ss;
			this.newSessionKey = newSessionKey;
		}

		public void doAction(WorkerThread workerThread) {
			this.sm.listener.forward(this.ss.getServerPortalPlayer().listener, this.ss.getServerSession());
		}
	}

	class JXIOPortalCallbacks implements ServerPortal.Callbacks {
		private final ServerPortalPlayer sm;

		public JXIOPortalCallbacks(ServerPortalPlayer sm) {
			this.sm = sm;
		}

		public void onSessionNew(long newSessionKey, String uriSrc, String srcIP) {
			LOG.info("onSessionNew: uri=" + uriSrc + ", srcaddr=" + srcIP);
			ServerPortalPlayer sp = workerThreads.getPortal();
			ServerSessionPlayer ss = new ServerSessionPlayer(sp, newSessionKey, srcIP);
			WorkerThread worker = workerThreads.getWorkerThread();
			worker.addWorkAction(ss.getAttachAction());
		}

        public void onSessionEvent(EventName session_event, String reason) {
			LOG.error("onSessionError: event='" + session_event.toString() + "', reason='" + reason + "'");
			System.exit(1);
		}

	}
}
