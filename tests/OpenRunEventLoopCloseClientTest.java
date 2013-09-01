//package clientTests;
import com.mellanox.*;

public class OpenRunEventLoopCloseClientTest implements Runnable{

	public void run(){
		///////////////////// Test 5 /////////////////////
		// Open a client, run event loop and close the client
		TestClient.print("*** Test 5: Open a client, run event loop and close the client *** ");
		
		// Setup parameters
		String url;
		JXIOEventQueueHandler eqh;
		MySesClient sClient;
		
		// Get url
		url = "rdma://" + TestClient.hostname + ":" + TestClient.port;
		
		// Setting up a JXIOEvent Queue Hanler
		eqh = new JXIOEventQueueHandler(TestClient.eqhSize);
		
		// Setting up a session client
		TestClient.print("----- Setting up a session client...");
		sClient = new MySesClient(eqh, url);
		
		// Run EQh
		TestClient.print("----- Run JXIOEvent Loop...");
		eqh.runEventLoop(1, 0);
		
		// Closing the session client
		TestClient.print("------ Closing the session client...");
		sClient.close();

		TestClient.setSuccess(5);
		TestClient.print("*** Test 5 Passed! *** ");
	}
}
