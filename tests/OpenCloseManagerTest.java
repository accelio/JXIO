//package managerTests;

import com.mellanox.jxio.*;

public class OpenCloseManagerTest implements Runnable {

	public void run(){
		///////////////////// Test 1 /////////////////////
		// Open and close a client 
		TestManager.print("*** Test 1: Open and close a manager *** ");
		
		// Setup parameters
		String url;
		EventQueueHandler eqh;
		MySesManager sManager;
		
		// Get url
		url = "rdma://" + TestManager.hostname + ":" + TestManager.port;
		
		// Setting up a Event Queue Hanler
		eqh = new EventQueueHandler();
		
		// Setting up a session client
		TestManager.print("----- Setting up a session manager...");
		sManager = new MySesManager(eqh, url);

		// Run EQH
		TestClient.print("----- Run Event Loop...");
		eqh.runEventLoop(1, -1 /* Infinite */);
		
		// Closing the session client
		TestManager.print("------ Closing the session manager...");
		sManager.close();

		TestManager.setSuccess(1);
		TestManager.print("*** Test 1 Passed! *** ");
	}
}
