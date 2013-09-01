//package managerTests;
import com.mellanox.*;

public class OpenRunEventLoopCloseManagerTest implements Runnable{

	public void run(){
		///////////////////// Test 5 /////////////////////
		// Open a client, run event loop and close the client
		TestManager.print("*** Test 5: Open a client, run event loop and close the client *** ");
		
		// Setup parameters
		String url;
		JXIOEventQueueHandler eqh;
		MySesManager sManager;
		
		// Get url
		url = "rdma://" + TestManager.hostname + ":" + TestManager.port;
		
		// Setting up a JXIOEvent Queue Hanler
		eqh = new JXIOEventQueueHandler(TestManager.eqhSize);
		
		// Setting up a session manager
		TestManager.print("----- Setting up a session manager...");
		sManager = new MySesManager(eqh, url);
		
		// Run EQh
		TestManager.print("----- Run JXIOEvent Loop...");
		eqh.runEventLoop(1, 0);
		
		// Closing the session manager
		TestManager.print("------ Closing the session manager...");
		sManager.close();

		TestManager.setSuccess(5);
		TestManager.print("*** Test 5 Passed! *** ");
	}
}
