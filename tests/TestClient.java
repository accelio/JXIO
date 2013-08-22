import java.util.logging.Level;
import java.util.Random;
import com.mellanox.JXLog;


public class TestClient {

	public static int numberOfTests = 3;
	private static JXLog testLog = JXLog.getLog(TestClient.class.getCanonicalName());
	private static boolean[] successIndicators = new boolean[3];
	private static MySesClient sClient;
	private static MySesClient[] sClientArray;
	private static int numOfSessionClients = 3;
	public static String url;
	public static int port;
	public static int portRange = 1234;
	
	public static void main(String[] args) {
		if (args.length <= 0){
			print("[TEST ERROR] Missing argument.");
			help();
			return;
		} else if (args.length > 1){
			print("[TEST ERROR] Too many arguments.");
			help();
			return;
		} else if (args[0].equals("--help")){
				help();
              	return;
		} else {
			// Run Tests
			print("\n*** Starting a Session Client Test ***");
			// Setting up and Event Queue Handler
			print("\n----- Setting up and Event Queue Handler...");
			int size = 1000; // TODO Why does it even need a size if it is not used?
			MyEQH eqh = new MyEQH(size);
			if (args[0].equals("--1")){
				clientTest1(eqh);
				report();
				return;
			} else if (args[0].equals("--2")){
				clientTest2(eqh);
				report();
				return;
			} else if (args[0].equals("--3")){
				clientTest3(eqh);
				report();
				return;
			}  else if (args[0].equals("--all")){
				clientTest1(eqh);
				clientTest2(eqh);
				clientTest3(eqh);
				report();
				return;
			}
		}
	}
	
	private static void clientTest1(MyEQH eqh){
		///////////////////// Test 1 /////////////////////
		// Open and close a client 
		print("*** Test 1: Open and close a client *** ");
		
		// Setting up a session client
		print("----- Setting up a session client...");
		url = "36.0.0.114"; 
		port = 1234;
		sClient = new MySesClient(eqh, url, port);
		
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
		
		// Setting up a session client
		print("----- Setting up a session client...");
		url = "0.0.0.0"; 
		port = 1234;
		sClient = new MySesClient(eqh, url, port);
		
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
		url = "36.0.0.114"; 
		Random portGenerator = new Random();
		sClientArray = new MySesClient[numOfSessionClients];
		for (int i = 0; i < numOfSessionClients; i++){
			port = portGenerator.nextInt(portRange) + 1;
			sClientArray[i] = new MySesClient(eqh, url, port);
		}
		
		// Closing the session clients
		print("------ Closing the session client...");
		for (int i = 0; i < numOfSessionClients; i++){
			sClientArray[i].close();
		}
		
		successIndicators[2] = true;
		print("*** Test 3 Passed! *** ");
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
	
	public static void help(){
		print("Usage: run_test [--options]\nWhere options include\n--all		Run all tests \n--<n>		Run test number <n> \n--help 	Show this help");
	}
	
	private static void print(String str){
		System.out.println("\n" + str + "\n");
		//testLog.log(Level.INFO, str);
	}

}