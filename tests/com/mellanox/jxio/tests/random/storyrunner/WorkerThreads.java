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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerThreads {

	private final WorkerThread[]  workers;
	private final ExecutorService executor;
	private final int             num_workers;
	private int                   next_worker_index = -1;

	public WorkerThreads(int num_eqhc) {
		super();
		this.num_workers = num_eqhc;
		this.executor = Executors.newCachedThreadPool();
		if (this.num_workers != -1) {
			this.workers = new WorkerThread[num_eqhc];
			this.next_worker_index = 0;
		} else {
			this.workers = null;
		}
	}

	public void close() {
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
	}

	public WorkerThread getWorkerThread() {

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
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		return worker;
	}
}
