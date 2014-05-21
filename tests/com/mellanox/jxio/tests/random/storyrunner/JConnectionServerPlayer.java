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
import com.mellanox.jxio.jxioConnection.JxioConnectionServer;

public class JConnectionServerPlayer extends GeneralPlayer {

	private final static Log     LOG = LogFactory.getLog(JConnectionServerPlayer.class.getSimpleName());

	private JxioConnectionServer conServer;
	private final int            id;
	private final String         name;
	private final URI            uri;
	private final int            numWorkers;
	private final long           runDurationSec;
	private final long           startDelaySec;
	private WorkerThread         workerThread;
	private final long    msgPoolMem;
	private long                 seed;
	private int                  violent_exit;
	private int                  rate;

	public JConnectionServerPlayer(int numWorkers, int id, int instance, URI uri, long startDelaySec, long runDurationSec,
	        WorkerThreads workerThreads, long msgPoolMem, int violent_exit, long seed, int msgRate) {
		System.out.println("ISServerPlayer started");
		this.name = new String("SPP[" + id + ":" + instance + "]");
		this.id = id;
		this.uri = uri;
		this.numWorkers = numWorkers;
		this.runDurationSec = runDurationSec;
		this.startDelaySec = startDelaySec;
		this.msgPoolMem = msgPoolMem;
		this.seed = seed;
		this.rate = msgRate;
		this.violent_exit = violent_exit;
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
		// register initialize timer
		TimerList.Timer tInitialize = new InitializeTimer(this.startDelaySec * 1000000);
		this.workerThread.start(tInitialize);
	}

	@Override
	protected void initialize() {
		conServer = new JxioConnectionServer(uri, msgPoolMem, numWorkers, new JConnectionInputServerReader(rate, seed));
		conServer.start();
		// register terminate timer
		TimerList.Timer tTerminate = new TerminateTimer(this.runDurationSec * 1000000);
		this.workerThread.start(tTerminate);
	}

	@Override
	protected void terminate() {
		if (this.violent_exit == 1) {
			LOG.info(this.toString() + ": terminating. Exiting NOW");
			System.exit(0);
		}
		LOG.info(this.toString() + ": terminating");
		conServer.disconnect();
	}
}
