//package managerTests;

import java.util.logging.Level;
import java.util.Random;
import com.mellanox.EventQueueHandler;
import com.mellanox.JXLog;


public class TestManager {
	
	// Manager Parameters
	public static String hostname;
	public static int port;
	public static int portRange = 1234;
	public static int eqhSize = 1000;
	// General Parameters
	private static int requestedTest;
	public static int numberOfTests = 5;
	private static boolean[] successIndicators = new boolean[numberOfTests];
	// Log
	private static JXLog testLog = JXLog.getLog(TestManager.class.getCanonicalName());
	
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
			
			print("*** Starting a Session Manager Test ***");
			// Setting up and Event Queue Handler
			// Run Tests
			Runnable test;
			switch(requestedTest){
			case 0: test = new OpenCloseManagerTest();
					test.run();
					test = new NonExistingHostnameManagerTest();
					test.run();
					test = new MutipleManagersOnSameEQHTest();
					test.run();
					test = new OpenRunEventLoopCloseManagerTest();
					test.run();
					test = new MutipleThreadsManager();
					test.run();
					report();
					return;
			case 1:	test = new OpenCloseManagerTest();
					test.run();
					report();
					return;
			case 2:	test = new NonExistingHostnameManagerTest();
					test.run();
					report();
					return;
			case 3: test = new MutipleManagersOnSameEQHTest();
					test.run();
					report();
					return;
			case 4: test = new OpenRunEventLoopCloseManagerTest();
					test.run();
					report();
					return;
			case 5: test = new MutipleThreadsManager();
					test.run();
					report();
						return;
			default: print("[TEST ERROR] Unknow test number.");
					return;
			}
		}
	}
	
	class MyThread implements Runnable{
		
		EventQueueHandler eqh;
		String url;
		MySesManager sManager;
		
		public MyThread(String caption, EventQueueHandler eqh, String url) {
			this.eqh = eqh;
			this.url = url;
		}
		
		public void run(){
			// Setting up a session manager
			print("----- Setting up a session manager...");
			sManager = new MySesManager(eqh, url);
			
			// Run Event Loop
			eqh.runEventLoop(1, 0);
			
			// Closing the session manager
			print("------ Closing the session manager...");
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
	
	public static void usage(){
		print("Usage: ./runManagerTest.sh <HOSTNAME> <PORT> [test]\nWhere [test] includes:\n0		Run all tests \n<n>		Run test number <n>\n");
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