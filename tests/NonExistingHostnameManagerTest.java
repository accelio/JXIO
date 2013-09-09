//package managerTests;

import com.mellanox.*;

public class NonExistingHostnameManagerTest implements Runnable {

	public void run(){
		///////////////////// Test 2 /////////////////////
		// A non existing IP address
		TestManager.print("*** Test 2: A non existing IP address *** ");
		
		// Setup parameters
		String url;
		JXIOEventQueueHandler eqh;
		MySesManager sManager;
		
		// Get url
		url = "rdma://" + "1.0.0.0" + ":" + TestManager.port;
		
		// Setting up a Event Queue Hanler
		eqh = new JXIOEventQueueHandler();
		
		// Setting up a session manager
		TestManager.print("----- Setting up a session manager...");
		sManager = new MySesManager(eqh, url);
		
		// Closing the session manager
		TestManager.print("------ Closing the session manager...");
		sManager.close();

		TestManager.setSuccess(2);
		TestManager.print("*** Test 2 Passed! *** ");
	}
}
