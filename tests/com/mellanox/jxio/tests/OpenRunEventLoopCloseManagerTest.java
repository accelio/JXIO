package com.mellanox.jxio.tests;

import com.mellanox.jxio.*;

public class OpenRunEventLoopCloseManagerTest implements Runnable {

	public final int port;
	
	public OpenRunEventLoopCloseManagerTest(int port) {
		this.port = port;
	}
	
	public void run(){
		///////////////////// Test 5 Helper //////////////
		// Multiple session manager on seperate EQH thread
		/*
		TestManager.print("*** Test 5 Helper: Multiple session manager on seperate EQH thread *** ");
		
		// Setup parameters
		String url;
		EventQueueHandler eqh;
		MySesManager sManager;
		
		// Get url
		url = "rdma://" + TestManager.hostname + ":" + this.port;
		
		TestManager.print("----- Setting up a event queue handler...");
		eqh = new EventQueueHandler();
		
		TestManager.print("----- Setting up a session manager...");
		sManager = new MySesManager(eqh, url);
		
		TestManager.print("----- Run Event Loop...for 1 event or 1 sec");
		eqh.runEventLoop(1, 1000000 /*1sec);
		
		TestManager.print("------ Closing the session manager...");
		sManager.close();

		TestManager.print("----- Closing the event queue handler...");
		eqh.close();

// Parant test will set as OK
//		TestManager.setSuccess(5);
		TestManager.print("*** Test 5 Helper Passed! *** ");
		*/
	}
}
