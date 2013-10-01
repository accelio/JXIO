//package managerTests;

import com.mellanox.jxio.*;

public class NonExistingHostnameManagerTest implements Runnable {

	public void run(){
		///////////////////// Test 2 /////////////////////
		// A non existing IP address
		TestManager.print("*** Test 2: A non existing IP address *** ");
		
		// Setup parameters
		String url;
		EventQueueHandler eqh;
		MySesManager sManager;
		
		// Get url
		url = "rdma://" + "1.0.0.0" + ":" + TestManager.port;
		
		// Setting up a Event Queue Hanler
		eqh = new EventQueueHandler();
		
		// Setting up a session manager
		TestManager.print("----- Setting up a session manager...");
		sManager = new MySesManager(eqh, url);

		// Run EQH
		TestClient.print("----- Run Event Loop...");
		eqh.runEventLoop(1, -1 /* Infinite */);
		
		// Closing the session manager
		TestManager.print("------ Closing the session manager...");
		sManager.close();

		TestManager.setSuccess(2);
		TestManager.print("*** Test 2 Passed! *** ");
	}
}
