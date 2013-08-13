import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import com.mellanox.*;


public class ClientControlPathSimpleTest {
	
	private static JXLog logger = JXLog.getLog(ClientControlPathSimpleTest.class.getCanonicalName());

	
	public static void main(String[] args){
		
		String url = args[0];
		String port = args[1];
		String combined_url = "rdma://"+url+":"+port;
		MySesClient ses;
		EventQueueHandler eventQHndl;
		
		int num_times = 1;
		
		eventQHndl = new EventQueueHandler (1000);
		
		for (int i=0; i<num_times; i++){
			
			ses = new MySesClient(eventQHndl, url,Integer.parseInt(port));//combined_url);
			eventQHndl.addEventHandler (ses);
			Thread t = new Thread (eventQHndl);
			t.start();
			eventQHndl.run();
			//for checking if server sends hello
			ses.close();
			
		}
		eventQHndl.close();

	}
}
