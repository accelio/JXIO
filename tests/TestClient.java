//package clientTests;

import java.util.logging.Level;

import com.mellanox.EventQueueHandler;
import com.mellanox.JXLog;


public class TestClient {
	
	// Client Parameters
	public static String hostname;
	public static int port;
	public static int portRange = 1234;
	public static int eqhSize = 1000;
	// General Parameters
	private static int requestedTest;
	public static int numberOfTests = 5;
	private static Runnable[] tests = new Runnable[numberOfTests + 1];
	private static boolean[] successIndicators = new boolean[numberOfTests];
	// Log
	private static JXLog testLog = JXLog.getLog(TestClient.class.getCanonicalName());
	
	public static void main(String[] args) {
		
		// Check arguments
		if (! argsCheck(args)){
			return;
		} else {
			//Configure Tests
			configure();
			
			// Get Hostname and Port
			hostname = args[0];
			port = Integer.parseInt(args[1]);
			// Get requested tests
			requestedTest = Integer.parseInt(args[2]);
			
			print("*** Starting a Session Client Test ***");
			// Run Tests
			if (requestedTest > numberOfTests){
				print("[TEST ERROR] Unknow test number.");
				return;
			}
			if (requestedTest == 0){
				for (int i = 1; i <= numberOfTests; i++){
					tests[i].run();
				}
				report();
			} else {
				tests[requestedTest].run();
				report();
			}
		}
	}
	
	private static void configure(){
		tests[1] = new OpenCloseClientTest();
		tests[2] = new NonExistingHostnameClientTest();
		tests[3] = new MutipleClientsOnSameEQHTest();
		tests[4] = new OpenRunEventLoopCloseClientTest();
		tests[5] = new MutipleThreadsClient();
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
		//System.out.println("\n" + str + "\n");
		testLog.log(Level.INFO, str);
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