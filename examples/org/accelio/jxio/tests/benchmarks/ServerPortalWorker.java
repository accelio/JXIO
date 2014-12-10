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

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.MsgPool;
import org.accelio.jxio.ServerPortal;
import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.WorkerCache.Worker;
import org.accelio.jxio.tests.benchmarks.DataPathTestServer;

public class ServerPortalWorker extends Thread implements Comparable<ServerPortalWorker>, Worker {

	private final ServerPortal sp;
	private final EventQueueHandler eqh;
	private final MsgPool pool;
	private final int portal_index;
	private AtomicInteger num_of_sessions;

	// logger
	private final static Log LOG = LogFactory.getLog(ServerPortalWorker.class.getCanonicalName());

	// cTor
	public ServerPortalWorker(int index, int inMsg_size, int outMsg_size, URI uri, int num_of_buffers, ServerPortal.Callbacks c) {
		portal_index = index;
		eqh = new EventQueueHandler(new ServerEQHCallbacks());
		pool = new MsgPool(num_of_buffers, inMsg_size, outMsg_size);
		eqh.bindMsgPool(pool);
		sp = new ServerPortal(eqh, uri, c);
		num_of_sessions = new AtomicInteger(0);
	}

	public void run() {
		System.out.println("Server worker number " + (portal_index + 1) + " is up and waiting for requests");
		// start running the event loop
		LOG.debug("starting eqh number " + portal_index + 1);
		eqh.run();
	}

	public ServerPortal getPortal() {
		return sp;
	}

	public void incrNumOfSessions() {
		num_of_sessions.incrementAndGet();
		System.out.println("Server worker number " + (portal_index + 1) + " got new Session, now handling " + num_of_sessions + " sessions");
	}

	private void decrNumOfSessions() {
		num_of_sessions.decrementAndGet();
		System.out.println("Server worker number " + (portal_index + 1) + " disconnected from a Session, now handling " + num_of_sessions
		        + " sessions");
		DataPathTestServer.updateWorkers(this); 
	}

	public void sessionClosed() {
		decrNumOfSessions();
	}
	
	// callbacks for the Server's event queue handler
	public class ServerEQHCallbacks implements EventQueueHandler.Callbacks {
		// this method should return an unbinded MsgPool.
		public MsgPool getAdditionalMsgPool(int inSize, int outSize) {
			System.out.println("Messages in Client's message pool ran out, Aborting test");
			return null;
		}
	}
	
	@Override
	public int compareTo(ServerPortalWorker s) {
		if (this.num_of_sessions.get() <= s.num_of_sessions.get()) {
			return -1;
		} else {
			return 1;
		}
	}

	@Override
    public boolean isFree() {
	    return true;
    }
}