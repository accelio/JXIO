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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WorkerThreads {

	private final static Log                LOG               = LogFactory.getLog(WorkerThreads.class.getSimpleName());
	private final WorkerThread[]            workers;
	private final ExecutorService           executor;
	private final Random                    rand;
	private final int                       num_workers;
	private int                             next_worker_index = -1;
	private ArrayList<ServerPortalPlayer>[] listPortalPlayers;
	private int                             index             = 0;
	private Map<Integer, Integer>           idToIndex         = new HashMap<Integer, Integer>();
	// represents worker threads that were created
	private int                             actualWorkersNumber;

	@SuppressWarnings("unchecked")
	public WorkerThreads(int numEQHs, int numListners) {
		super();
		this.num_workers = numEQHs;
		this.executor = Executors.newCachedThreadPool();
		if (this.num_workers != -1) {
			this.workers = new WorkerThread[numEQHs];
			this.next_worker_index = 0;
		} else {
			this.workers = null;
		}
		this.listPortalPlayers = (ArrayList<ServerPortalPlayer>[]) new ArrayList[numListners];
		for (int i = 0; i < numListners; i++) {
			this.listPortalPlayers[i] = new ArrayList<ServerPortalPlayer>();
		}

		this.rand = new Random();
	}

	public void close() {

		for (int i = 0; i < actualWorkersNumber; i++) {
			workers[i].notifyClose();
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
	}

	public void addPortal(int id, ServerPortalPlayer spp) {
		synchronized (listPortalPlayers[index]) {
			this.listPortalPlayers[index].add(spp);
			if (!idToIndex.containsKey(id)){
				idToIndex.put(id, index);
				index++;
			}
		}
	}

	public ServerPortalPlayer getPortal(int id) {
		ServerPortalPlayer spp = null;
		synchronized (listPortalPlayers[idToIndex.get(id)]) {
			int index = this.rand.nextInt(listPortalPlayers[idToIndex.get(id)].size());
			spp = listPortalPlayers[idToIndex.get(id)].get(index);
			LOG.debug("chosen index is " + index);
		}
		return spp;
	}

	public synchronized WorkerThread getWorkerThread() {

		WorkerThread worker = getWorkerThreadByIndex(this.next_worker_index);

		if (this.num_workers != -1) {
			workers[this.next_worker_index] = worker;
			this.next_worker_index++;
			if (this.next_worker_index >= this.num_workers)
				this.next_worker_index = 0;
		}
		return worker;
	}

	public WorkerThread getWorkerThreadByIndex(int worker_index) {
		WorkerThread worker = null;
		if (this.num_workers != -1 && this.num_workers > worker_index) {
			// if collection is limited then take from it the next slot
			worker = this.workers[worker_index];
		}
		if (worker == null) {
			// allocate new WorkerThread in case we are in unlimited workers mode
			// or if we did not populate the WorkerThread slot yet
			worker = createWorkerThread();
		}
		return worker;
	}

	private WorkerThread createWorkerThread() {
		WorkerThread worker = new WorkerThread();
		executor.execute(worker);
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		actualWorkersNumber++;
		return worker;
	}
}
