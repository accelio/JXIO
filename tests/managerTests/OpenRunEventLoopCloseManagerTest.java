package managerTests;

import Tests.MyEQH;
import Tests.MySesManager;

public class OpenRunEventLoopCloseManagerTest implements Runnable{

	public void run(){
		///////////////////// Test 5 /////////////////////
		// Open a client, run event loop and close the client
		TestManager.print("*** Test 5: Open a client, run event loop and close the client *** ");
		
		// Setup parameters
		String url;
		MyEQH eqh;
		MySesManager sManager;
		
		// Get url
		url = "rdma://" + TestManager.hostname + ":" + TestManager.port;
		
		// Setting up a Event Queue Hanler
		eqh = new MyEQH(TestManager.eqhSize);
		
		// Setting up a session manager
		TestManager.print("----- Setting up a session manager...");
		sManager = new MySesManager(eqh, url);
		
		// Run EQh
		TestManager.print("----- Run Event Loop...");
		eqh.runEventLoop(1, 0);
		
		// Closing the session manager
		TestManager.print("------ Closing the session manager...");
		sManager.close();

		TestManager.setSuccess(5);
		TestManager.print("*** Test 5 Passed! *** ");
	}
}
