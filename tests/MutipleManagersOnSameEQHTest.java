//package managerTests;

import com.mellanox.*;
import java.util.Random;


public class MutipleManagersOnSameEQHTest implements Runnable{

	public void run(){
		///////////////////// Test 3 /////////////////////
		// Multiple session manager on the same EQH
		TestManager.print("*** Test 3: Multiple session client on the same EQH *** ");
		
		// Setup Multiple Clients Parameters
		String url;
		JXIOEventQueueHandler eqh;
		MySesManager[] sManagerArray;
		int numOfSessionManagers = 3;
		
		// Setting up a Event Queue Hanler
		eqh = new JXIOEventQueueHandler(TestManager.eqhSize);
		
		// Setting up a multiple session managers
		TestManager.print("----- Setting up a multiple session managers...");
		Random portGenerator = new Random();
		sManagerArray = new MySesManager[numOfSessionManagers];
		for (int i = 0; i < numOfSessionManagers; i++){
			// Randomize Port
			int port = portGenerator.nextInt(TestManager.portRange) + 1;
			
			// Get url
			url = "rdma://" + TestManager.hostname + ":" + port;		
			
			// Setup Manager
			sManagerArray[i] = new MySesManager(eqh, url);
		}
		
		// Closing the session managers
		TestManager.print("------ Closing the session manager...");
		for (int i = 0; i < numOfSessionManagers; i++){
			sManagerArray[i].close();
		}
		
		TestManager.setSuccess(3);
		TestManager.print("*** Test 3 Passed! *** ");
	}
}
