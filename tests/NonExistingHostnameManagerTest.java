//package managerTests;

import com.mellanox.jxio.*;

public class NonExistingHostnameManagerTest implements Runnable {

	public final int port;
	
	public NonExistingHostnameManagerTest(int port) {
		this.port = port;
	}
	
	public void run(){
		///////////////////// Test 2 /////////////////////
		// A non existing IP address
		TestManager.print("*** Test 2: A non existing IP address *** ");
		
		// Setup parameters
		String url;
		EventQueueHandler eqh;
		MySesManager sManager;
		
		// Get url
		url = "rdma://" + "1.0.0.0" + ":" + this.port;
		
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

		TestManager.setSuccess(2);
		TestManager.print("*** Test 2 Passed! *** ");
//TODO ALEXR: need to fix test so it realy catches the failure to create (bind) the MySesManager corerctly.
	}
}