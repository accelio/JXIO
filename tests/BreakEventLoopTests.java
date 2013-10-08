//package tests;

import com.mellanox.jxio.EventQueueHandler;

public class BreakEventLoopTests implements Runnable {

	public class InnerEQHThread implements Runnable {

		private volatile boolean exitFlag = false;
		private int loops = 0;
		EventQueueHandler eqh;
		
		public void run() {
			TestManager.print("----- Setting up a event queue handler...(from thread = " + Thread.currentThread().getId() + ")");
			eqh = new EventQueueHandler();
			
			while (!exitFlag) {
				TestManager.print("----- Continue Event Loop in blocking mode...");
				eqh.runEventLoop(1, -1); // Blocking! No events should trigger exit from loop unless broken on purpose 

				loops++; // count the number of loops
				TestManager.print("----- Got out of Event Loop...(Loops = " + loops + ")");
			}
			TestManager.print("----- Closing the event queue handler...");
			eqh.close();

			TestManager.print("----- Exiting up a event queue handler...(from thread = " + Thread.currentThread().getId() + ")");
		}
		
		public void stop() {
			TestManager.print("----- Stopping running thread with Event Loop (from thread = " + Thread.currentThread().getId() + ")");
			exitFlag = true;
			wakeup();
			TestManager.print("----- Stopped running thread with Event Loop (from thread = " + Thread.currentThread().getId() + ")");
		}

		public void wakeup() {
			TestManager.print("----- Breaking Event Loop (from thread = " + Thread.currentThread().getId() + ")");
			eqh.breakEventLoop();
		}
	}

	public void run() {
		///////////////////// Test 6 /////////////////////
		// Break Event Loop
		TestManager.print("*** Test 6: Break Event Loop *** ");

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

		if (eqh1.loops == wakeup) {
			TestManager.setSuccess(6);
			TestManager.print("*** Test 6 Passed! *** ");
		}
		else {
			TestManager.print("*** Test 6 Failed! *** (wrong number of wakeup times (internal thread="+eqh1.loops+", wakeup called="+wakeup+")");
		}
	}
}
