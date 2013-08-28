package clientTests;

import Tests.MyEQH;
import Tests.MySesClient;

public class OpenCloseClientTest implements Runnable{

	public void run(){
		///////////////////// Test 1 /////////////////////
		// Open and close a client 
		TestClient.print("*** Test 1: Open and close a client *** ");
		
		// Setup parameters
		String url;
		MyEQH eqh;
		MySesClient sClient;
		
		// Get url
		url = "rdma://" + TestClient.hostname + ":" + TestClient.port;
		
		// Setting up a Event Queue Hanler
		eqh = new MyEQH(TestClient.eqhSize);
		
		// Setting up a session client
		TestClient.print("----- Setting up a session client...");
		sClient = new MySesClient(eqh, url);
		
		// Closing the session client
		TestClient.print("------ Closing the session client...");
		sClient.close();

		TestClient.setSuccess(1);
		TestClient.print("*** Test 1 Passed! *** ");
	}
}
