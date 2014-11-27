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
package org.accelio.jxio.tests.benchmarks;

import org.accelio.jxio.EventQueueHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.tests.benchmarks.StatClientSession;

public class StatTest implements Runnable {

	static final int         NUM_ITER = 100;
	int                      num_sessions;
	int                      port;
	String                   hostname;
	private final static Log LOG      = LogFactory.getLog(StatTest.class.getCanonicalName());

	public StatTest(String hostname, int port, int num_sessions) {
		this.hostname = hostname;
		this.port = port;
		this.num_sessions = num_sessions;

	}

	void print(String str) {
		LOG.debug("********" + str);
	}

	public void run() {

		// Setup parameters
		EventQueueHandler eqh = new EventQueueHandler();
		String url = "rdma://" + hostname + ":" + port;
		// Need to make sure that GC will not run
		StatClientSession[] clArr = new StatClientSession[NUM_ITER];

		long startTime = System.nanoTime();
		for (int i = 0; i < NUM_ITER; i++) {
			print("NUM_ITER = " + i);
			clArr[i] = new StatClientSession(eqh, url, num_sessions);

			eqh.runEventLoop(EventQueueHandler.INFINITE_EVENTS, EventQueueHandler.INFINITE_DURATION);
		}

		long endTime = System.nanoTime();
		double sec = (double) (endTime - startTime) / 1000000000 / NUM_ITER;
		System.out.println(" It took " + sec + " seconds for " + num_sessions + " sessions");
		eqh.close();
	}
}
