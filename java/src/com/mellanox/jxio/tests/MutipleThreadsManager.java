package com.mellanox.jxio.tests;


public class MutipleThreadsManager implements Runnable{

	public final int port;
	
	public MutipleThreadsManager(int port) {
		this.port = port;
	}
	
	public void run() {
		///////////////////// Test 5 /////////////////////
		// Multiple session manager on seperate EQH thread
		TestManager.print("*** Test 5: Multiple session manager on seperate EQH thread *** ");

		// Setup tests
		Runnable ct1 = new OpenRunEventLoopCloseManagerTest(this.port + 1);
		Runnable ct2 = new OpenRunEventLoopCloseManagerTest(this.port + 2);
		Runnable ct3 = new OpenRunEventLoopCloseManagerTest(this.port + 3);
		Runnable ct4 = new OpenRunEventLoopCloseManagerTest(this.port + 4);
		Runnable ct5 = new OpenRunEventLoopCloseManagerTest(this.port + 5);
		Runnable ct6 = new OpenRunEventLoopCloseManagerTest(this.port + 6);
		
		Thread t1 = new Thread(ct1);
		Thread t2 = new Thread(ct2);
		Thread t3 = new Thread(ct3);
		Thread t4 = new Thread(ct4);
		Thread t5 = new Thread(ct5);
		Thread t6 = new Thread(ct6);
		
		t1.start();
		t2.start();
		t3.start();
		t4.start();
		t5.start();
		t6.start();
		
		// Wait for theard to end
		try{
			t1.join();
			t2.join();
			t3.join();
			t4.join();
			t5.join();
			t6.join();
		} catch (InterruptedException e){
			
		}
		
		TestManager.setSuccess(5);
		TestManager.print("*** Test 5 Passed! *** ");
	}
}
