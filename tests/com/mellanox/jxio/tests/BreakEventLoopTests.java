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
		public int loops = 0;
		public int wakeups = 0;
		EventQueueHandler eqh;
		
		public void run() {
			print("----- Setting up a event queue handler...");
			eqh = new EventQueueHandler(null);
			
			checkEQHBreakFromOtherThread();
			checkEQHBreakFromSelfThread();
			checkEQHTimeout();

			print("----- Closing the event queue handler...");
			eqh.close();
			print("----- Exiting up a event queue handler...");
		}
		
		public void stop() {
			print("----- Stopping running thread with event loop");
			exitFlag = true;
			wakeup();
			print("----- Stopped running thread with event loop");
		}

		public void wakeup() {
			wakeups++;
			print("----- Breaking event loop (wakeups = " + wakeups + ")");
			eqh.breakEventLoop();
		}

		private void blockOnEQH() {
			print("----- Continue event loop in blocking mode...");
			eqh.runEventLoop(1, -1); // Blocking! No events should trigger exit from loop unless broken on purpose 
			loops++; // count the number of loops
			print("----- Got out of event loop...(loops = " + loops + ")");
		}

		private void checkEQHBreakFromOtherThread() {
			print("--- Testing EQH.wakeup() from other thread...");
			while (!exitFlag) {
				blockOnEQH(); // Main thread will be calling eqh.wakeup() while this thread is blocking on EQH
			}
		}

		private void checkEQHBreakFromSelfThread() {
			print("--- Testing EQH.wakeup() from self thread...");
			wakeup(); // This thread is calling eqh.wakeup() before going to block on EQH
			blockOnEQH();
		}

		private void checkEQHTimeout() {
			long timeout = 10000; // 10 msec
			print("--- Testing EQH (" + timeout/1000 + " msec sleep)...");
			long start = System.nanoTime()/1000;
			eqh.runEventLoop(1, timeout); // Blocking with Timeout!
			long duration = System.nanoTime()/1000 - start;
			long delta = duration - timeout;
			long abs_delta = (delta < 0) ? -delta : delta;
			if (abs_delta > 1000) {
				printFailureAndExit("(it took too much time to wake up from EQH (blocked for " + delta + " usec more then requested)");
			} 
			else {
				print("----- Woken by timeout correctly after " + timeout/1000 + " msec (with " + delta + " usec offset) ...");
			}
		}	
	}

	public void run() {
		// Break Event Loop
		System.out.println("*** Test: Break Event Loop *** ");

		// Setup tests
		InnerEQHThread eqh1 = new InnerEQHThread();
		Thread t1 = new Thread(eqh1);
		t1.start();
		
		try {
			Thread.sleep(2000);

			eqh1.wakeup();
			Thread.sleep(20);

			eqh1.wakeup();
			Thread.sleep(200);

			eqh1.wakeup();
			Thread.sleep(20);

			eqh1.wakeup();
			Thread.sleep(1000);

			eqh1.stop();

			// Wait for thread to end
			t1.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		if (eqh1.loops != eqh1.wakeups) {
			printFailureAndExit("wrong number of wakeup times (internal thread="+eqh1.loops+", wakeup called="+eqh1.wakeups+")");
		} else {
			System.out.println("*** Test Passed! *** ");
		}
	}
	
	public static void main(String[] args) {
	    BreakEventLoopTests test = new BreakEventLoopTests();
	    test.run();
    }
	
	private void print(String str) {
		System.out.println("[tid=" + Thread.currentThread().getId() + "] " + str);
	}

	private void printFailureAndExit(String str) {
		System.out.println("*** Test FAILED! *** " + str);
		System.exit(1);
	}
}
