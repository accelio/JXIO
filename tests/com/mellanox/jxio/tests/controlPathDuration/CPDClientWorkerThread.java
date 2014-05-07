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

package com.mellanox.jxio.tests.controlPathDuration;

import java.net.URI;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.ClientSession;
import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.EventReason;
import com.mellanox.jxio.Msg;

public class CPDClientWorkerThread extends Thread {
	private final ClientSession     cs[];
	private final EventQueueHandler eqh;
	private final long              timeMeasurements[][];
	private final int               numClients;
	private final Thread            thread;
	private int                     counterClosed;
	private final URI               uri;
	private final boolean           multiThread;
	private final int               firstIndex;
	private static Log              LOG = LogFactory.getLog(CPDClientWorkerThread.class.getCanonicalName());

	public CPDClientWorkerThread(URI uri, int numWorkers, boolean multiThread, long[][] timeMeasurements, int firstIndex) {
		this.timeMeasurements = timeMeasurements;
		this.numClients = numWorkers;
		this.uri = uri;
		this.firstIndex = firstIndex;
		this.multiThread = multiThread;
		this.cs = new ClientSession[numWorkers];
		this.eqh = new EventQueueHandler(null);
		this.thread = new Thread(this.eqh);
	}

	public void run() {

		for (int i = 0; i < numClients; i++) {
			this.timeMeasurements[firstIndex + i][0] = System.nanoTime();
			this.cs[i] = new ClientSession(eqh, uri, new ClientWorkerCallbacks(this, i, firstIndex + i));
			this.timeMeasurements[firstIndex + i][1] = System.nanoTime();
			this.eqh.runEventLoop(100, 0);
		}
		this.eqh.run();
		this.eqh.close();
	}

	class ClientWorkerCallbacks implements ClientSession.Callbacks {

		private final CPDClientWorkerThread client;
		private int                         index;
		private int                         rowIndex;

		ClientWorkerCallbacks(CPDClientWorkerThread client, int index, int rowIndex) {
			this.client = client;
			this.index = index;
			this.rowIndex = rowIndex;
		}

		public void onMsgError(Msg msg, EventReason reason) {
		}

		public void onSessionEstablished() {
			client.timeMeasurements[rowIndex][2] = System.nanoTime();
			cs[index].close();
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			if (session_event == EventName.SESSION_CLOSED) {
				client.timeMeasurements[rowIndex][3] = System.nanoTime();
				client.counterClosed++;
				if (client.counterClosed == client.numClients) {
					client.eqh.stop();
				}
			}
		}

		public void onResponse(Msg msg) {
		}
	}
}
