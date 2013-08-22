import com.mellanox.*;



public class ServerControlPathSimpleTest {
private static JXLog logger = JXLog.getLog(ServerControlPathSimpleTest.class.getCanonicalName());

	
	public static void main(String[] args){
		
		String url = args[0];
		String port = args[1];
		String combined_url = "rdma://"+url+":"+port;
		MySesClient ses;
		EventQueueHandler eventQHndl = new MyEQH(1000);


		ServerManager man = new MySesManager(eventQHndl, url);
		
		man.run(); //will wait for on_session event
	}
		
}
