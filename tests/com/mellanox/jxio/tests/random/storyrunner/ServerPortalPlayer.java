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
import com.mellanox.jxio.ServerSession;

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
	private long                         seed;

	public ServerPortalPlayer(int numWorkers, int id, int instance, URI uri, long startDelaySec, long runDurationSec,
	        WorkerThreads workerThreads, ArrayList<MsgPoolData> msgPoolsData, long seed) {
		this.name = new String("SPP[" + id + ":" + instance + "]");
		this.id = id;
		this.uri = uri;
		this.numWorkers = numWorkers;
		this.runDurationSec = runDurationSec;
		this.startDelaySec = startDelaySec;
		this.workerThreads = workerThreads;
		this.msgPoolsData = msgPoolsData;
		msgPools = new ArrayList<MsgPool>();
		this.seed = seed;
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
			LOG.info(this.toString() + ": new MsgPool: " + pool);
			msgPools.add(pool);
			workerThread.getEQH().bindMsgPool(pool);
			workerThread.updateMsgPoolSize(p.getInSize(), p.getOutSize());
		}

		// register initialize timer
		TimerList.Timer tInitialize = new InitializeTimer(this.startDelaySec * 1000000);
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
		this.listener = new ServerPortal(this.workerThread.getEQH(), uri, new JXIOPortalCallbacks());
		LOG.debug(this.toString() + ": server listening on '" + this.listener.getUriForServer() + "'");

		// register server in available list of portals
		this.workerThreads.addPortal(this.id, this);

		// register terminate timer
		TimerList.Timer tTerminate = new TerminateTimer(this.runDurationSec * 1000000);
		this.workerThread.start(tTerminate);

		// workers
		for (int i = 0; i < numWorkers; i++) {
			ServerPortalPlayer spp = new ServerPortalPlayer(0, this.id, i + 1, this.listener.getUriForServer(), 0,
			        runDurationSec, workerThreads, msgPoolsData, seed + (i * 47));
			workerThreads.getWorkerThread().addWorkAction(spp.getAttachAction());
		}
	}

	@Override
	protected void terminate() {
		LOG.info(this.toString() + ": terminating");
		this.listener.close();
		// isclosing = true?
	}

	public void notifyReadyforWork(ServerSessionPlayer ss, long newSessionKey) {
		CompleteOnNewSessionForward action = new CompleteOnNewSessionForward(ss, newSessionKey);
		this.workerThread.addWorkAction(action);
	}

	public class CompleteOnNewSessionForward implements WorkerThread.QueueAction {
		private final ServerPortalPlayer  outer = ServerPortalPlayer.this;
		private final ServerSessionPlayer ssp;
		private final long                newSessionKey;

		public CompleteOnNewSessionForward(ServerSessionPlayer ssp, long newSessionKey) {
			this.ssp = ssp;
			this.newSessionKey = newSessionKey;
		}

		public void doAction(WorkerThread workerThread) {
			outer.listener.forward(this.ssp.getServerPortalPlayer().listener, this.ssp.getServerSession());
		}
	}

	class JXIOPortalCallbacks implements ServerPortal.Callbacks {
		private final ServerPortalPlayer outer = ServerPortalPlayer.this;

		public void onSessionNew(ServerSession.SessionKey sesKey, String srcIP) {
			String srcUri = sesKey.getUri();
			LOG.info(outer.toString() + ": onSessionNew: uri=" + srcUri + ", srcaddr=" + srcIP);
			String clientName = srcUri.substring(srcUri.indexOf("name=") + 5);
			if (srcUri.contains("reject=1")) {

				LOG.info("Rejecting session from '" + clientName + "'");
				outer.listener.reject(sesKey, EventReason.NOT_SUPPORTED, "");
				return;
			}

			LOG.info(outer.toString() + ": Establishing new session from '" + clientName + "'");
			ServerPortalPlayer sp = workerThreads.getPortal(outer.id);
			ServerSessionPlayer ss = new ServerSessionPlayer(sp, sesKey, srcIP, seed);
			outer.listener.forward(sp.listener, ss.getServerSession());
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			
			switch (session_event){
				case SESSION_CLOSED:
					LOG.info(outer.toString() + ": SESSION_TEARDOWN. reason='" + reason + "'");
					break;
				case PORTAL_CLOSED:
					LOG.info(outer.toString() + ": PORTAL_CLOSED, reason='" + reason + "'");
					for (MsgPool pool : msgPools){
						outer.workerThread.getEQH().releaseMsgPool(pool);
						pool.deleteMsgPool();
					}
					break;
				default:
					LOG.error(outer.toString() + ": FAILURE, onSessionError: event='" + session_event + "', reason='"
					        + reason + "'");
					System.exit(1);
					break;
				
			}
		}
	}
}
