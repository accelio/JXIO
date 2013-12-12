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
			System.out.println("*** Test Failed! *** (wrong number of wakeup times (internal thread="+eqh1.loops+", wakeup called="+wakeup+")");
		}
	}
}
