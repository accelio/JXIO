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
import java.net.URISyntaxException;

public class StoryRunner {

	public static StoryRunner    story = new StoryRunner();
	private static WorkerThreads workers;

	public static void main(String[] args) throws InterruptedException {

		// define number of worker thread ('-1' for infinite)
		story.workers = new WorkerThreads(-1);
//		story.workers = new WorkerThreads(4);

		try {
			ServerManagerPlayer sm = new ServerManagerPlayer(new URI("rdma://0:52002/"), 0, 12);
			workers.getWorkerThread().addWorkAction(sm.getAttachAction());
			
			Thread.sleep(10);

			ClientPlayer c1 = new ClientPlayer(new URI("rdma://0:52002/"), 0, 6, 2);
			workers.getWorkerThread().addWorkAction(c1.getAttachAction());

			ClientPlayer c2 = new ClientPlayer(new URI("rdma://0:52002/"), 4, 6, 3);
			workers.getWorkerThread().addWorkAction(c2.getAttachAction());

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public WorkerThreads getWorkerThreads() {
		return story.workers;
	}
}
