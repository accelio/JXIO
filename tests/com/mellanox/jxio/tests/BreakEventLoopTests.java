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
package com.mellanox.jxio.tests;

import com.mellanox.jxio.EventQueueHandler;

public class BreakEventLoopTests implements Runnable {

	public class InnerEQHThread implements Runnable {

		private volatile boolean exitFlag = false;
		private int loops = 0;
		EventQueueHandler eqh;
		
		public void run() {
			System.out.println("----- Setting up a event queue handler...(from thread = " + Thread.currentThread().getId() + ")");
			eqh = new EventQueueHandler();
			
			while (!exitFlag) {
				System.out.println("----- Continue Event Loop in blocking mode...");
				eqh.runEventLoop(1, -1); // Blocking! No events should trigger exit from loop unless broken on purpose 

				loops++; // count the number of loops
				System.out.println("----- Got out of Event Loop...(Loops = " + loops + ")");
			}

			System.out.println("----- Event Loop sleep test (10 msec sleep)...");
			long timeout = 10000; // 10 msec
			long start = System.nanoTime()/1000;
			eqh.runEventLoop(1, timeout); // Blocking with Timeout!
			long duration = System.nanoTime()/1000 - start;
			long delta = duration - timeout;
			long abs_delta = (delta < 0) ? -delta : delta;
			if (abs_delta > 1000) {
				System.out.println("*** Test FAILED! *** (it took too much time to wake up from EQH (blocked for " + delta + " usec more then requested)");
				System.exit(1);
			} 
			else {
				System.out.println("----- Woken by timeout correctly after " + delta + " usec...");
			}

			System.out.println("----- Closing the event queue handler...");
			eqh.close();

			System.out.println("----- Exiting up a event queue handler...(from thread = " + Thread.currentThread().getId() + ")");
		}
		
		public void stop() {
			System.out.println("----- Stopping running thread with Event Loop (from thread = " + Thread.currentThread().getId() + ")");
			exitFlag = true;
			wakeup();
			System.out.println("----- Stopped running thread with Event Loop (from thread = " + Thread.currentThread().getId() + ")");
		}

		public void wakeup() {
			System.out.println("----- Breaking Event Loop (from thread = " + Thread.currentThread().getId() + ")");
			eqh.breakEventLoop();
		}
	}

	public void run() {
		// Break Event Loop
		System.out.println("*** Test: Break Event Loop *** ");

		// Setup tests
		InnerEQHThread eqh1 = new InnerEQHThread();
		Thread t1 = new Thread(eqh1);
		t1.start();
		
		int wakeup = 0;

		try {
			Thread.sleep(2000);

			eqh1.wakeup();
			wakeup++;
			Thread.sleep(20);

			eqh1.wakeup();
			wakeup++;
			Thread.sleep(200);

			eqh1.wakeup();
			wakeup++;
			Thread.sleep(20);

			eqh1.wakeup();
			wakeup++;
			Thread.sleep(1000);

			eqh1.stop();
			wakeup++;

			// Wait for thread to end
			t1.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (eqh1.loops == wakeup) {;
			System.out.println("*** Test Passed! *** ");
		}
		else {
			System.out.println("*** Test FAILED! *** (wrong number of wakeup times (internal thread="+eqh1.loops+", wakeup called="+wakeup+")");
		}
	}
	
	public static void main(String[] args) {
	    BreakEventLoopTests test = new BreakEventLoopTests();
	    test.run();
    }
}
