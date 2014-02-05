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
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WorkerThreads {

	private final static Log                LOG               = LogFactory.getLog(WorkerThreads.class.getSimpleName());
	private final ArrayList<WorkerThread>   workers           = new ArrayList<WorkerThread>();
	private final ExecutorService           executor;
	private final Random                    rand;
	private final int                       num_workers;
	private int                             next_worker_index = 0;
	private ArrayList<ServerPortalPlayer>[] listPortalPlayers;
	private int                             index             = 0;
	private Map<Integer, Integer>           idToIndex         = new HashMap<Integer, Integer>();

	@SuppressWarnings("unchecked")
	public WorkerThreads(int numEQHs, int numListners) {
		super();
		this.num_workers = numEQHs;
		this.executor = Executors.newCachedThreadPool();
		this.listPortalPlayers = (ArrayList<ServerPortalPlayer>[]) new ArrayList[numListners];
		for (int i = 0; i < numListners; i++) {
			this.listPortalPlayers[i] = new ArrayList<ServerPortalPlayer>();
		}

		this.rand = new Random();
	}

	public void close() {

		for (WorkerThread workerThread : workers) {
			workerThread.notifyClose();
		}

		executor.shutdown();
		while (!executor.isTerminated()) {
		}
	}

	public void addPortal(int id, ServerPortalPlayer spp) {
		synchronized (listPortalPlayers[index]) {
			this.listPortalPlayers[index].add(spp);
			if (!idToIndex.containsKey(id)) {
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
		WorkerThread worker = null;
		if (this.num_workers == -1) {
			// unlimited worker mode
			worker = createWorkerThread();
			workers.add(worker);
		} else {
			if (this.workers.size() < this.num_workers) {
				// the slop hasn't been populated yet
				worker = createWorkerThread();
				workers.add(worker);
			} else {
				worker = this.workers.get(this.next_worker_index);
			}
			this.next_worker_index++;
			// round robin
			this.next_worker_index = this.next_worker_index % this.num_workers;
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
		return worker;
	}
}
