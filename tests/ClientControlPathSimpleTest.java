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
		
		int num_times = 2;
		
//		List<MySesClient> clientArray = new ArrayList<MySesClient>();

		eventQHndl = new EventQueueHandler ();
		
		for (int i=0; i<num_times; i++){
			
			ses = new MySesClient(eventQHndl, url,Integer.parseInt(port));//combined_url);
			eventQHndl.addSession (ses);
			eventQHndl.runEventLoop(1, 0);
			//for checking if server sends hello
			eventQHndl.runEventLoop(1, 0);
			ses.close();
			
		}
		eventQHndl.close();

	}
}
