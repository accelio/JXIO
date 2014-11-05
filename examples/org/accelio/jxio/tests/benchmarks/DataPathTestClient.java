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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.tests.benchmarks.ClientWorker;
import org.accelio.jxio.tests.benchmarks.DataPathTest;

public class DataPathTestClient extends DataPathTest {

	private int test_iterations;

	// workers list (each worker is a future task that will run in a separate thread)
	protected final List<FutureTask<double[]>> workers;

	// future tasks executer
	protected final ExecutorService executor;

	// calculation members
	double totalBTW = 0;
	int totalCnt = 0;
	int totalPPS = 0;

	// results matrix
	private double results[][];

	// logger
	private final static Log LOG = LogFactory.getLog(DataPathTestClient.class.getCanonicalName());
	
	private boolean write_to_file = false;

	// cTor
	public DataPathTestClient(String[] args) {
		super(args);
		test_iterations = Integer.parseInt(args[7]);
		file_path = args[6];
		results = new double[num_of_threads][test_iterations * 3];
		workers = new ArrayList<FutureTask<double[]>>(num_of_threads);
		executor = Executors.newFixedThreadPool(num_of_threads);

		for (int i = 0; i < num_of_threads; i++) {
			ClientWorker cw = new ClientWorker(inMsg_size, outMsg_size, uri, burst_size, results[i]);
			workers.add(new FutureTask<double[]>(cw));
		}
		// Create/Open file
		if (!file_path.equals("no_file")) {
			write_to_file = true;
			try {
				fstream = new FileWriter(file_path, true);
				out = new BufferedWriter(fstream);
			} catch (Exception e) {
				LOG.error("Error in opening the results file");
				e.printStackTrace();
			}
		}
	}

	public void runTest() {
		for (int i = 0; i < workers.size(); i++) {
			System.out.println("running client task number " + (i + 1));
			executor.execute(workers.get(i));
		}
		for (int i = 0; i < workers.size(); i++) {
			try {
				System.out.println("gathering results from tasks");
				results[i] = workers.get(i).get();
			} catch (InterruptedException e) {
				LOG.error("Thread was interrupted before returning results");
				e.printStackTrace();
			} catch (ExecutionException e) {
				LOG.error("Thread was aborted, no results available");
				e.printStackTrace();
			}
		}
		// process results from threads and write them to file
		this.processResults();
		// shutdown all threads
		this.executor.shutdown();
	}

	private void processResults() {
		try {
			System.out.println("processing results");
			double totalTPS[] = new double[test_iterations];
			double totalOutBW[] = new double[test_iterations];
			double totalInBW[] = new double[test_iterations];
			double av_TPS = 0;
			double av_OutBW = 0;
			double av_InBW = 0;
			//for(int i=0; i<results.length; i++) {
			//	System.out.println(Arrays.toString(results[i]));
			//}
			for (int j = 0; j < (test_iterations * 3) - 1; j += 3) {
				for (int i = 0; i < workers.size(); i++) {
					totalTPS[j / 3] += results[i][j];
					totalOutBW[j / 3] += results[i][j + 1];
					totalInBW[j / 3] += results[i][j + 2];
				}
			}
			
			for (int i = 0; i < test_iterations; i++) {
				av_TPS += totalTPS[i];
				av_OutBW += totalOutBW[i];
				av_InBW += totalInBW[i];
			}

			av_TPS = av_TPS / test_iterations;
			av_OutBW = av_OutBW / test_iterations;
			av_InBW = av_InBW / test_iterations;

			System.out.println("average_TPS = " + av_TPS + ",  average_RX_BW = " + av_InBW + " MB,  average_TX_BW = " + av_OutBW + " MB,  in_msg_size = "
			        + inMsg_size + " Bytes,  out_msg_size = " + outMsg_size + " Bytes");
			// write results to file
			this.writeResultsToFile(av_TPS, av_OutBW, av_InBW);

		} catch (Exception e) {
			LOG.error("error in calculation, no results available");
			e.printStackTrace();
		}
	}

	// writes test results into file path given in command line
	private void writeResultsToFile(double av_TPS, double av_OutBW, double av_InBW) {
		if (write_to_file) {
			try {
				out.write(inMsg_size + "," + outMsg_size + "," + av_TPS + "," + av_OutBW + "," + av_InBW + "\n");
				out.close();
			} catch (IOException e) {
				LOG.error("error in writing results to file : " + file_path);
				e.printStackTrace();
			}
		} else {
			return;
		}
	}

	public static void main(String[] args) {
		DataPathTestClient test = new DataPathTestClient(args);
		test.runTest();
	}

}