package com.mellanox.jxio.tests;


public class MutipleThreadsClient implements Runnable{

	public void run(){
		///////////////////// Test 5 /////////////////////
		// Multiple threads on the same EQH
		TestClient.print("*** Test 4: Multipule threads on the same EQH*** ");

		// Setup tests
		Runnable ct1 = new OpenRunEventLoopCloseClientTest();
		Runnable ct2 = new OpenRunEventLoopCloseClientTest();
		Runnable ct3 = new OpenRunEventLoopCloseClientTest();
		Runnable ct4 = new OpenRunEventLoopCloseClientTest();
		Runnable ct5 = new OpenRunEventLoopCloseClientTest();
		Runnable ct6 = new OpenRunEventLoopCloseClientTest();
		
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
		
		TestClient.setSuccess(5);
		TestClient.print("*** Test 5 Passed! *** ");
	}
}
