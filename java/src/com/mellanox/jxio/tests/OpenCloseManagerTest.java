package com.mellanox.jxio.tests;

import com.mellanox.jxio.*;

public class OpenCloseManagerTest implements Runnable {

	public final int port;
	
	public OpenCloseManagerTest(int port) {
		this.port = port;
	}
	
	public void run() {
		///////////////////// Test 1 /////////////////////
		// Open and close a client 
		TestManager.print("*** Test 1: Open and close a manager *** ");
		
		// Setup parameters
		String url;
		EventQueueHandler eqh;
		MySesManager sManager;
		
		// Get url
		url = "rdma://" + TestManager.hostname + ":" + this.port;
		
		TestManager.print("----- Setting up a event queue handler...");
		eqh = new EventQueueHandler();
		
		TestManager.print("----- Setting up a session manager (url=" + url + ")...");
		sManager = new MySesManager(eqh, url);

		TestManager.print("----- Run Event Loop...for 1 event or 1 sec");
		eqh.runEventLoop(1, 1000000 /*1sec*/);
		
		TestManager.print("------ Closing the session manager...");
		sManager.close();

		TestManager.print("----- Closing the event queue handler...");
		eqh.close();

		TestManager.setSuccess(1);
		TestManager.print("*** Test 1 Passed! *** ");
	}
}
