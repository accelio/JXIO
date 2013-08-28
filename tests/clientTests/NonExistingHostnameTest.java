package clientTests;

import Tests.MyEQH;
import Tests.MySesClient;

public class NonExistingHostnameTest implements Runnable{

	public void run(){
		///////////////////// Test 2 /////////////////////
		// A non existing IP address
		TestClient.print("*** Test 2: A non existing IP address *** ");
		
		// Setup parameters
		String url;
		MyEQH eqh;
		MySesClient sClient;
		
		// Get url
		url = "rdma://" + "0.0.0.0" + ":" + TestClient.port;
		
		// Setting up a Event Queue Hanler
		eqh = new MyEQH(TestClient.eqhSize);
		
		// Setting up a session client
		TestClient.print("----- Setting up a session client...");
		sClient = new MySesClient(eqh, url);
		
		// Closing the session client
		TestClient.print("------ Closing the session client...");
		sClient.close();

		TestClient.setSuccess(2);
		TestClient.print("*** Test 2 Passed! *** ");
	}
}
