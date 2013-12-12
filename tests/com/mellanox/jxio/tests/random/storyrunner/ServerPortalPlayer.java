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
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.ServerPortal;
import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventReason;
import com.mellanox.jxio.MsgPool;

public class ServerPortalPlayer extends GeneralPlayer {

	private final static Log             LOG = LogFactory.getLog(ServerPortalPlayer.class.getSimpleName());

	private final int                    id;
	private final String                 name;
	private final URI                    uri;
	private final int                    numWorkers;
	private final long                   runDurationSec;
	private final long                   startDelaySec;
	private WorkerThread                 workerThread;
	private WorkerThreads                workerThreads;
	private ServerPortal                 listener;
	private final ArrayList<MsgPoolData> msgPoolsData;
	private ArrayList<MsgPool>           msgPools;

	public ServerPortalPlayer(int numWorkers, int id, int instance, URI uri, long startDelaySec, long runDurationSec,
	        WorkerThreads workerThreads, ArrayList<MsgPoolData> msgPoolsData) {
		this.name = new String("SPP[" + id + ":" + instance + "]");
		this.id = id;
		this.uri = uri;
		this.numWorkers = numWorkers;
		this.runDurationSec = runDurationSec;
		this.startDelaySec = startDelaySec;
		this.workerThreads = workerThreads;
		this.msgPoolsData = msgPoolsData;
		msgPools = new ArrayList<MsgPool>();
		LOG.debug("new " + this.toString() + " done");
	}

	public String toString() {
		return name;
	}

	WorkerThread getWorkerThread() {
		return workerThread;
	}

	@Override
	public void attach(WorkerThread workerThread) {
		LOG.info(this.toString() + ": attaching to WorkerThread '" + workerThread.toString() + "'" + ", startDelay = "
		        + startDelaySec + "sec, runDuration = " + runDurationSec + "sec");

		this.workerThread = workerThread;

		for (MsgPoolData p : msgPoolsData) {
			MsgPool pool = new MsgPool(p.getCount(), p.getInSize(), p.getOutSize());
			msgPools.add(pool);
			workerThread.getEQH().bindMsgPool(pool);
		}

		// register initialize timer
		TimerList.Timer tInitialize = new InitializeTimer(this, this.startDelaySec * 1000000);
		this.workerThread.start(tInitialize);
	}

	URI getUriForServer() {
		return this.listener.getUriForServer();
	}

	@Override
	protected void initialize() {
		LOG.debug(this.toString() + ": initializing");

		// start server listener
		LOG.info(this.toString() + ": starting server listener on '" + uri.getHost() + ":" + uri.getPort() + "'");
		this.listener = new ServerPortal(this.workerThread.getEQH(), uri, new JXIOPortalCallbacks(this));
		LOG.debug(this.toString() + ": server listening on '" + this.listener.getUriForServer() + "'");

		// register server in available list of portals
		this.workerThreads.addPortal(this.id, this);

		// register terminate timer
		TimerList.Timer tTerminate = new TerminateTimer(this, this.runDurationSec * 1000000);
		this.workerThread.start(tTerminate);

		// workers
		for (int i = 0; i < numWorkers; i++) {
			ServerPortalPlayer spp = new ServerPortalPlayer(0, this.id, i + 1, this.listener.getUriForServer(), 0,
			        runDurationSec, workerThreads, msgPoolsData);
			workerThreads.getWorkerThread().addWorkAction(spp.getAttachAction());
		}
	}

	@Override
	protected void terminate() {
		LOG.info(this.toString() + ": terminating");
		this.listener.close();
		for (MsgPool pool : msgPools) {
			this.workerThread.getEQH().releaseMsgPool(pool);
			pool.deleteMsgPool();
		}
		// isclosing = true?
	}

	public void notifyReadyforWork(ServerSessionPlayer ss, long newSessionKey) {
		CompleteOnNewSessionForward action = new CompleteOnNewSessionForward(this, ss, newSessionKey);
		this.workerThread.addWorkAction(action);
	}

	public class CompleteOnNewSessionForward implements WorkerThread.QueueAction {
		private final ServerPortalPlayer  spp;
		private final ServerSessionPlayer ssp;
		private final long                newSessionKey;

		public CompleteOnNewSessionForward(ServerPortalPlayer spp, ServerSessionPlayer ssp, long newSessionKey) {
			this.spp = spp;
			this.ssp = ssp;
			this.newSessionKey = newSessionKey;
		}

		public void doAction(WorkerThread workerThread) {
			this.spp.listener.forward(this.ssp.getServerPortalPlayer().listener, this.ssp.getServerSession());
		}
	}

	class JXIOPortalCallbacks implements ServerPortal.Callbacks {
		private final ServerPortalPlayer spp;

		public JXIOPortalCallbacks(ServerPortalPlayer spp) {
			this.spp = spp;
		}

		public void onSessionNew(long newSessionKey, String uriSrc, String srcIP) {
			LOG.info(spp.toString() + ": onSessionNew: uri=" + uriSrc + ", srcaddr=" + srcIP);
			String clientName = uriSrc.substring(uriSrc.indexOf("name=") + 5);
			if (uriSrc.contains("rejectme=1")) {

				LOG.info("Rejecting session from '" + clientName + "'");
				this.spp.listener.reject(newSessionKey, EventReason.NOT_SUPPORTED, "");
				return;
			}

			LOG.info("Establishing new session from '" + clientName + "'");
			ServerPortalPlayer sp = workerThreads.getPortal(this.spp.id);
			ServerSessionPlayer ss = new ServerSessionPlayer(sp, newSessionKey, srcIP);
			this.spp.listener.forward(sp.listener, ss.getServerSession());
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			if (session_event == EventName.SESSION_TEARDOWN) {
				LOG.info(spp.toString() + ": SESSION_TEARDOWN. reason='" + reason.toString() + "'");
			} else {
				LOG.error(spp.toString() + ": onSessionError: event='" + session_event.toString() + "', reason='"
				        + reason.toString() + "'");
				System.exit(1);
			}
		}

	}
}
