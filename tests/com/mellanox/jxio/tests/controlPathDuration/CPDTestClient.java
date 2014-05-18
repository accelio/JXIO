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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.net.URI;
import java.net.URISyntaxException;

public class CPDTestClient {

	static final int                          numSteps = 4;
	private long                              results[][];
	// workers list (each will run in a separate thread)
	private final List<CPDClientWorkerThread> workers;
	private boolean                           multiThread;
	private int                               clientsNum;
	private URI                               uri      = null;
	private final static Log                  LOG      = LogFactory.getLog(CPDTestClient.class.getCanonicalName());

	// cTor
	public CPDTestClient(String[] args) {
		this.parseCommandArgs(args);
		// There are 2 modes:
		// multiThread - there are clientsNum threads, each running one Client
		// singleThread - there is 1 thread running clientsNum clients
		int numThreads = (multiThread) ? clientsNum : 1;
		int clientsPerThread = (multiThread) ? 1 : clientsNum;

		results = new long[clientsNum][numSteps];
		workers = new ArrayList<CPDClientWorkerThread>(numThreads);

		for (int i = 0; i < numThreads; i++) {
			// each client writes the measurements directly to results array, starting from i row.
			CPDClientWorkerThread cw = new CPDClientWorkerThread(this.uri, clientsPerThread, this.multiThread,
			        this.results, i);
			workers.add(cw);
		}
	}

	public void runTest() {
		for (Thread thread : workers) {
			thread.start();
		}
		for (Thread thread : workers) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				LOG.error("Thread was interrupted before returning results");
				e.printStackTrace();
			}
		}

		this.parseTimeMeasurements();
	}

	// parses and prints time measurements
	private void parseTimeMeasurements() {
		long average[], max[], min[];
		average = new long[numSteps - 1];
		max = new long[numSteps - 1];
		min = new long[numSteps - 1];
		for (int j = 0; j < numSteps - 1; j++) {
			min[j] = results[0][j + 1] - results[0][j];
			for (int i = 0; i < results.length; i++) {
				long temp = results[i][j + 1] - results[i][j];
				if (temp > max[j]) {
					max[j] = temp;
				}
				if (temp < min[j]) {
					min[j] = temp;
				}
				average[j] += temp;
			}
			average[j] = average[j] / results.length;
		}
		String steps[] = new String[numSteps - 1];
		steps[0] = "Session Initialization";
		steps[1] = "Session Establishment";
		steps[2] = "Session Closing";
		System.out.println(String.format("%-24s %10s %10s %10s %10s", "  step", "average", "min", "max", "units"));
		System.out.println(String.format("%-24s %10s %10s %10s %10s", "  ----", "-------", "---", "---", "-----"));
		for (int i = 0; i < numSteps - 1; i++) {
			System.out.println(String.format("%-24s %10d %10d %10d %10s", steps[i], average[i]/1000, min[i]/1000, max[i]/1000, "[usec]"));
//			System.out.println(steps[i] + " took on average " + average[i] / 1000 + ", min " + min[i] / 1000 + ", max "
//			        + max[i] / 1000 + " [usec]");
		}

	}

	public static void main(String[] args) {
		CPDTestClient test = new CPDTestClient(args);
		test.runTest();
	}

	private void parseCommandArgs(String[] args) {
		String server_ip = args[0];
		String server_port = args[1];
		multiThread = (Integer.parseInt(args[2]) == 1) ? true : false;
		clientsNum = Integer.parseInt(args[3]);
		String url_string = "rdma://" + server_ip + ":" + server_port;
		try {
			this.uri = new URI(url_string);
		} catch (URISyntaxException e) {
			LOG.error("Bad URI, Aborting test...");
			System.exit(1);
		}
	}

}
