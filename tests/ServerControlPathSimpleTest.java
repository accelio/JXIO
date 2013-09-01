import com.mellanox.*;



public class ServerControlPathSimpleTest {
private static JXLog logger = JXLog.getLog(ServerControlPathSimpleTest.class.getCanonicalName());

	
	public static void main(String[] args){
		
		String url = args[0];
		String port = args[1];
		String combined_url = "rdma://"+url+":"+port;
		MySesClient ses;
		EventQueueHandler eventQHndl = new EventQueueHandler(500);


		ServerManager man = new MySesManager(eventQHndl, url);
		
		eventQHndl.run(); 
	}
		
}
