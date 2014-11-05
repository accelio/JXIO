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

public class StatMTMain {
	
	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.println("StatMTMain $IP $PORT $NUM_SESSIONS $NUM_THREADS");
			return;
		}
		
		String IP = args[0];
		int port  = Integer.parseInt(args[1]);
		int num_sessions = Integer.parseInt(args[2]);
		int num_threads = Integer.parseInt(args[3]);
		Thread[] t_arr = new Thread[num_threads];
		StatTest[] mt_arr = new StatTest[num_threads];
		
		for (int i=0; i<num_threads; i++) {
			mt_arr[i] = new StatTest(IP, port, (num_sessions/num_threads));
			t_arr[i] = new Thread(mt_arr[i]);
			t_arr[i].start();
        }
	}
}
