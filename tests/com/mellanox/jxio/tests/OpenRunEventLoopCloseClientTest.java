package com.mellanox.jxio.tests;


import com.mellanox.jxio.*;

public class OpenRunEventLoopCloseClientTest implements Runnable {

	public void run(){
		///////////////////// Test 4 /////////////////////
		// Open a client, run event loop and close the client
		TestClient.print("*** Test 4: Open a client, run event loop and close the client *** ");
		
		// Setup parameters
		String url;
		EventQueueHandler eqh;
		MySesClient sClient;
		
		// Get url
		url = "rdma://" + TestClient.hostname + ":" + TestClient.port;
		
		TestClient.print("----- Setting up a event queue handler...");
		eqh = new EventQueueHandler();
		
		TestClient.print("----- Setting up a session client...");
		sClient = new MySesClient(eqh, url);

		TestClient.print("----- Run Event Loop...for 1 event or 1 sec");
		eqh.runEventLoop(1, -1 /*1sec*/);
		
		TestClient.print("------ Closing the session client...");
		//
		
		sClient.close();

		TestClient.print("----- Closing the event queue handler...");
		eqh.close();

		TestClient.setSuccess(4);
		TestClient.print("*** Test 4 Passed! *** ");
	}
}
