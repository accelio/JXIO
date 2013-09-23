//package clientTests;

import com.mellanox.jxio.*;

public class ClientMsgTest implements Runnable {

	public void run(){
		///////////////////// Test 6 /////////////////////
		// Open and close a client 
		TestClient.print("*** Test 6: Open and close a client *** ");
		
		// Setup parameters
		String url;
		EventQueueHandler eqh;
		MySesClient sClient;
		MsgPool msgPool;
		Msg msg;
		
		// Get url
		url = "rdma://" + TestClient.hostname + ":" + TestClient.port;
		
		// Setting up a Event Queue Hanler
		eqh = new EventQueueHandler();
		
		// Setting up Msg 
		msgPool = new MsgPool(1,1,1);
		//msg = msgPool.getMsg();

		// Setting up a session client
		TestClient.print("----- Setting up a session client...");
		sClient = new MySesClient(eqh, url);
		
		
		// Closing the session client
		TestClient.print("------ Closing the session client...");
		sClient.close();

		TestClient.setSuccess(6);
		TestClient.print("*** Test 6 Passed! *** ");
	}
}
