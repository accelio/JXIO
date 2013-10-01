//package clientTests;

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
		
		// Setting up a Event Queue Hanler
		eqh = new EventQueueHandler();
		
		// Setting up a session client
		TestClient.print("----- Setting up a session client...");
		sClient = new MySesClient(eqh, url);

		// Run EQH
		TestClient.print("----- Run Event Loop...");
		eqh.runEventLoop(1000, -1 /* Infinite */);
		
		// Closing the session client
		TestClient.print("------ Closing the session client...");
		sClient.close();

		TestClient.setSuccess(4);
		TestClient.print("*** Test 4 Passed! *** ");
	}
}
