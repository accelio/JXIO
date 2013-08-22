import java.util.logging.Level;
import java.util.Random;
import com.mellanox.JXLog;


public class TestClient {
	
	// Client Parameters
	private static MySesClient sClient;
	public static String url;
	public static String hostname;
	public static int port;
	public static int portRange = 1234;
	// Multiple Clients Parameters
	private static MySesClient[] sClientArray;
	private static int numOfSessionClients = 3;
	// General Parameters
	private static JXLog testLog = JXLog.getLog(TestClient.class.getCanonicalName());
	private static int requestedTest;
	public static int numberOfTests = 3;
	private static boolean[] successIndicators = new boolean[3];
	
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
			case 0: clientTest1(eqh);
					clientTest2(eqh);
					clientTest3(eqh);
					report();
					return;
			case 1:	clientTest1(eqh);
					report();
					return;
			case 2:	clientTest2(eqh);
					report();
					return;
			case 3: clientTest3(eqh);
					report();
					return;
			default: print("[TEST ERROR] Unknow test number.");
					return;
			}
		}
	}
	
	private static void clientTest1(MyEQH eqh){
		///////////////////// Test 1 /////////////////////
		// Open and close a client 
		print("*** Test 1: Open and close a client *** ");
		
		// Get url
		url = "rdma://" + hostname + ":" + port;
		
		// Setting up a session client
		print("----- Setting up a session client...");
		sClient = new MySesClient(eqh, url);
		
		// Closing the session client
		print("------ Closing the session client...");
		sClient.close();

		successIndicators[0] = true;
		print("*** Test 1 Passed! *** ");
	}
	
	private static void clientTest2(MyEQH eqh){
		///////////////////// Test 2 /////////////////////
		// A non existing IP address
		print("*** Test 2: A non existing IP address *** ");
		
		// Get url
		url = "rdma://" + "0.0.0.0" + ":" + port;
		
		// Setting up a session client
		print("----- Setting up a session client...");
		sClient = new MySesClient(eqh, url);
		
		// Closing the session client
		print("------ Closing the session client...");
		sClient.close();

		successIndicators[1] = true;
		print("*** Test 2 Passed! *** ");
	}
	
	private static void clientTest3(MyEQH eqh){
		///////////////////// Test 3 /////////////////////
		// Multiple session client on the same EQH
		print("*** Test 3: Multiple session client on the same EQH *** ");
		
		// Setting up a multiple session clients
		print("----- Setting up a multiple session clients...");
		Random portGenerator = new Random();
		sClientArray = new MySesClient[numOfSessionClients];
		for (int i = 0; i < numOfSessionClients; i++){
			// Rnadomize Port
			port = portGenerator.nextInt(portRange) + 1;
			
			// Get url
			url = "rdma://" + hostname + ":" + port;
			
			sClientArray[i] = new MySesClient(eqh, url);
		}
		
		// Closing the session clients
		print("------ Closing the session client...");
		for (int i = 0; i < numOfSessionClients; i++){
			sClientArray[i].close();
		}
		
		successIndicators[2] = true;
		print("*** Test 3 Passed! *** ");
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
		print("Usage: ./runClientTest.sh <HOSTNAME> <PORT> [test]\nWhere [test] includes:\n0		Run all tests \n<n>		Run test number <n>\n");
	}
	
	private static void print(String str){
		System.out.println("\n" + str + "\n");
		//testLog.log(Level.INFO, str);
	}

}