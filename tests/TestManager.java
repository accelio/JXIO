package Tests;

import java.util.logging.Level;
import java.util.Random;
import com.mellanox.EventQueueHandler;
import com.mellanox.JXLog;


public class TestManager {
	
	// Manager Parameters
	public static String hostname;
	public static int port;
	public static int portRange = 1234;
	// General Parameters
	private static JXLog testLog = JXLog.getLog(TestManager.class.getCanonicalName());
	private static int requestedTest;
	public static int numberOfTests = 5;
	private static boolean[] successIndicators = new boolean[numberOfTests];
	
	public static void main(String[] args) {
		
		// Check arguments
		if (! argsCheck(args)){
			return;
		} else {
			
			// Get Hostname and Port
			hostname = args[0];
			port = Integer.parseInt(args[1]);
			// Get requested tests
			requestedTest = Integer.parseInt(args[2]);
			
			print("*** Starting a Session Client Test ***");
			// Setting up and Event Queue Handler
			print("----- Setting up and Event Queue Handler...");
			int size = 1000; // TODO Why does it even need a size if it is not used?
			MyEQH eqh = new MyEQH(size);
			// Run Tests
			switch(requestedTest){
			case 0: serverTest1(eqh);
					serverTest2(eqh);
					serverTest3(eqh);
					serverTest4();
					serverTest5(eqh);
					report();
					return;
			case 1:	serverTest1(eqh);
					report();
					return;
			case 2:	serverTest2(eqh);
					report();
					return;
			case 3: serverTest3(eqh);
					report();
					return;
			case 4: serverTest4();
					report();
					return;
			case 5: serverTest5(eqh);
					report();
					return;
			default: print("[TEST ERROR] Unknow test number.");
					return;
			}
		}
	}
	
	private static void serverTest1(MyEQH eqh){
		///////////////////// Test 1 /////////////////////
		// Open and close a server 
		print("*** Test 1: Open and close a server *** ");
		
		// Setup parameters
		MySesManager sManager;
		String url;
		
		// Get url
		url = "rdma://" + hostname + ":" + port;
		
		// Setting up a session server
		print("----- Setting up a session server...");
		sManager = new MySesManager(eqh, url);
		
		// Closing the session server
		print("------ Closing the session server...");
		sManager.close();

		successIndicators[0] = true;
		print("*** Test 1 Passed! *** ");
	}
	
	private static void serverTest2(MyEQH eqh){
		///////////////////// Test 2 /////////////////////
		// A non existing IP address
		print("*** Test 2: A non existing IP address *** ");
		
		// Setup parameters
		MySesManager sManager;
		String url;
		
		// Get url
		url = "rdma://" + "0.0.0.0" + ":" + port;
		
		// Setting up a session server
		print("----- Setting up a session server...");
		sManager = new MySesManager(eqh, url);
		
		// Closing the session server
		print("------ Closing the session server...");
		sManager.close();

		successIndicators[1] = true;
		print("*** Test 2 Passed! *** ");
	}
	
	private static void serverTest3(MyEQH eqh){
		///////////////////// Test 3 /////////////////////
		// Multiple session server on the same EQH
		print("*** Test 3: Multiple session managers on the same EQH *** ");
		
		// Setup Multiple servers Parameters
		MySesManager[] sManagerArray;
		int numOfSessionManagers = 3;
		String url;
		
		// Setting up a multiple session servers
		print("----- Setting up a multiple session servers...");
		Random portGenerator = new Random();
		sManagerArray = new MySesManager[numOfSessionManagers];
		for (int i = 0; i < numOfSessionManagers; i++){
			// Rnadomize Port
			port = portGenerator.nextInt(portRange) + 1;
			
			// Get url
			url = "rdma://" + hostname + ":" + port;
			
			sManagerArray[i] = new MySesManager(eqh, url);
		}
		
		// Closing the session servers
		print("------ Closing the session server...");
		for (int i = 0; i < numOfSessionManagers; i++){
			sManagerArray[i].close();
		}
		
		successIndicators[2] = true;
		print("*** Test 3 Passed! *** ");
	}
	
	private static void serverTest4(){
		///////////////////// Test 4 /////////////////////
		// Multipule threads on the same EQH
		print("*** Test 4: Multipule threads on the same EQH*** ");
		
		// Setup parameters
		String url;
		
		// Get url
		url = "rdma://" + hostname + ":" + port;
		
		TestManager ts = new TestManager();
		MyThread t1 = ts.new MyThread("t1", new MyEQH(1000), url);
		MyThread t2 = ts.new MyThread("t2", new MyEQH(1000), url);
		MyThread t3 = ts.new MyThread("t3", new MyEQH(1000), url);
		MyThread t4 = ts.new MyThread("t4", new MyEQH(1000), url);
		MyThread t5 = ts.new MyThread("t5", new MyEQH(1000), url);
		MyThread t6 = ts.new MyThread("t6", new MyEQH(1000), url);
		
		t1.start();
		t2.start();
		t3.start();
		t4.start();
		t5.start();
		t6.start();
		
		// Wait for theard to end
		try{
			t1.join();
			t2.join();
			t3.join();
			t4.join();
			t5.join();
			t6.join();
		} catch (InterruptedException e){
			
		}
		
		successIndicators[3] = true;
		print("*** Test 4 Passed! *** ");
	}
	
	private static void serverTest5(MyEQH eqh){
		///////////////////// Test 5 /////////////////////
		// Forwarding
		print("*** Test 1: Forwarding *** ");
		
		// Setup parameters
		MySesManager sManager;
		String url;
		
		// Get url
		url = "rdma://" + hostname + ":" + port;
		
		// Setting up a session server
		print("----- Setting up a session server...");
		sManager = new MySesManager(eqh, url);
		
		// Forward
		sManager.forward(new MySesServer(eqh, url), 1000);
		
		// Closing the session server
		print("------ Closing the session server...");
		sManager.close();

		successIndicators[0] = true;
		print("*** Test 1 Passed! *** ");
		
		
		
		successIndicators[4] = true;
		print("*** Test 5 Passed! *** ");
	}
	
	class MyThread extends Thread{
		
		EventQueueHandler eqh;
		String url;
		MySesManager sManager;
		
		public MyThread(String caption, EventQueueHandler eqh, String url) {
			super(caption);
			this.eqh = eqh;
			this.url = url;
		}
		
		public void run(){
			// Setting up a session server
			print("----- Setting up a session server...");
			sManager = new MySesManager(eqh, url);
			
			// Wait
			try{
				sleep((long)(Math.random()*1000));
			} catch (InterruptedException e){
				
			}
			
			// Closing the session server
			print("------ Closing the session server...");
			sManager.close();
		}
	}
	
	private static boolean argsCheck(String[] args){
		if (args.length <= 0){
			print("[TEST ERROR] Missing arguments.");
			usage();
			return false;
		} else if (args.length < 3){
			usage();
			return false;
		}
		return true;
	}
	
	private static void report(){
		String passed;
		String report = "Tests Report:\n=============\n";
		for (int i = 0; i < numberOfTests; i++){
			passed = successIndicators[i] ? "Passed" : "Failed";
			report += "Test " + (i+1) + " " + passed + "!\n";
		}
		print(report);
	}
	
	public static void usage(){
		print("Usage: ./runServerTest.sh <HOSTNAME> <PORT> [test]\nWhere [test] includes:\n0		Run all tests \n<n>		Run test number <n>\n");
	}
	
	private static void print(String str){
		System.out.println("\n" + str + "\n");
		//testLog.log(Level.INFO, str);
	}

}