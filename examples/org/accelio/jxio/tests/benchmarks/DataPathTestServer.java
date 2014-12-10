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

import java.util.PriorityQueue;

import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.ServerPortal;
import org.accelio.jxio.ServerSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.EventName;
import org.accelio.jxio.EventReason;
import org.accelio.jxio.WorkerCache.Worker;
import org.accelio.jxio.WorkerCache.WorkerProvider;
import org.accelio.jxio.tests.benchmarks.DataPathTest;
import org.accelio.jxio.tests.benchmarks.ServerPortalWorker;
import org.accelio.jxio.tests.benchmarks.ServerSessionHandle;

public class DataPathTestServer extends DataPathTest implements WorkerProvider {

	private final EventQueueHandler listen_eqh;
	private final TestServerCallbacks tsc;
	private final ServerPortal listener;

	// portal workers list
	private static PriorityQueue<ServerPortalWorker> SPWorkers;

	// logger
	private final static Log LOG = LogFactory.getLog(DataPathTestServer.class.getCanonicalName());

	// cTor
	public DataPathTestServer(String args[]) {
		super(args);
		listen_eqh = new EventQueueHandler(null);
		tsc = new TestServerCallbacks();
		listener = new ServerPortal(listen_eqh, uri, tsc, this);
		SPWorkers = new PriorityQueue<ServerPortalWorker>(num_of_threads);
		//adding 64 to num_of_buffers_per_thread due to ACCELLIO demand
		for (int i = 0; i < num_of_threads; i++) {
			SPWorkers.add(new ServerPortalWorker(i, inMsg_size, outMsg_size, listener.getUriForServer(), burst_size + 64, tsc));
		}
	}

	public void run_test() {
		System.out.println("Starting server portal workers");
		for (int i = 0; i < num_of_threads; i++) {
			for (ServerPortalWorker spw : SPWorkers) {
				spw.start();
			}
			listen_eqh.run();
		}
	}

	private synchronized static ServerPortalWorker getNextWorker() {
		// retrieve next spw and update its position in the queue
		ServerPortalWorker s = SPWorkers.poll();
		s.incrNumOfSessions();
		SPWorkers.add(s);
		return s;
	}

	public synchronized static void updateWorkers(ServerPortalWorker s) {
		// remove & add the ServerPortalWorker in order.
		SPWorkers.remove(s);
		SPWorkers.add(s);
	}

	// callbacks for the listener server portal
	public class TestServerCallbacks implements ServerPortal.Callbacks {

		public void onSessionNew(ServerSession.SessionKey sesKey, String srcIP, Worker hint) {
			if (hint == null) {
				listener.reject(sesKey, EventReason.CONNECT_ERROR, "No worker was found");
				return;
			}
			LOG.debug("New session created, forwarding to the next Server Portal");
			// forward the created session to the ServerPortal
			ServerPortalWorker spw = (ServerPortalWorker)hint;
			listener.forward(spw.getPortal(), (new ServerSessionHandle(sesKey, spw)).getSession());
		}

		public void onSessionEvent(EventName event, EventReason reason) {
			LOG.debug("GOT EVENT " + event.toString() + "because of " + reason.toString());
		}
	}
	
	// main
	public static void main(String[] args) {
		DataPathTestServer test = new DataPathTestServer(args);
		test.run_test();
	}

	@Override
    public Worker getWorker() {
	    return getNextWorker();
    }
}
