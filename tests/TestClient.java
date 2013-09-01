//package clientTests;

import java.util.logging.Level;
import java.util.Random;

import com.mellanox.JXIOEventQueueHandler;
import com.mellanox.JXIOLog;


public class TestClient {
	
	// Client Parameters
	public static String hostname;
	public static int port;
	public static int portRange = 1234;
	public static int eqhSize = 1000;
	// General Parameters
	private static int requestedTest;
	public static int numberOfTests = 5;
	private static boolean[] successIndicators = new boolean[numberOfTests];
	// Log
	private static JXIOLog testLog = JXIOLog.getLog(TestClient.class.getCanonicalName());
	
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
			// Setting up and JXIOEvent Queue Handler
			// Run Tests
			Runnable test;
			switch(requestedTest){
			case 0: test = new OpenCloseClientTest();
					test.run();
					test = new NonExistingHostnameClientTest();
					test.run();
					test = new MutipleClientsOnSameEQHTest();
					test.run();
					test = new OpenRunEventLoopCloseClientTest();
					test.run();
					test = new MutipleThreadsClient();
					test.run();
					report();
					return;
			case 1:	test = new OpenCloseClientTest();
					test.run();
					report();
					return;
			case 2:	test = new NonExistingHostnameClientTest();
					test.run();
					report();
					return;
			case 3: test = new MutipleClientsOnSameEQHTest();
					test.run();
					report();
					return;
			case 4: test = new OpenRunEventLoopCloseClientTest();
					test.run();
					report();
					return;
			case 5: test = new MutipleThreadsClient();
					test.run();
					report();
						return;
			default: print("[TEST ERROR] Unknow test number.");
					return;
			}
		}
	}
	
	class MyThread implements Runnable{
		
		JXIOEventQueueHandler eqh;
		String url;
		MySesClient sClient;
		
		public MyThread(String caption, JXIOEventQueueHandler eqh, String url) {
			this.eqh = eqh;
			this.url = url;
		}
		
		public void run(){
			// Setting up a session client
			print("----- Setting up a session client...");
			sClient = new MySesClient(eqh, url);
			
			// Run JXIOEvent Loop
			eqh.runEventLoop(1, 0);
			
			// Closing the session client
			print("------ Closing the session client...");
			sClient.close();
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
	
	public static void usage(){
		print("Usage: ./runClientTest.sh <HOSTNAME> <PORT> [test]\nWhere [test] includes:\n0		Run all tests \n<n>		Run test number <n>\n");
	}
	
	public static void print(String str){
		System.out.println("\n" + str + "\n");
		//testLog.log(Level.INFO, str);
	}
	
	public static void setSuccess(int test){
		successIndicators[test - 1] = true;
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

}