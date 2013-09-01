import com.mellanox.*;



public class ServerControlPathSimpleTest {
private static JXIOLog logger = JXIOLog.getLog(ServerControlPathSimpleTest.class.getCanonicalName());

	
	public static void main(String[] args){
		
		String url = args[0];
		String port = args[1];
		String combined_url = "rdma://"+url+":"+port;
		MySesClient ses;
		JXIOEventQueueHandler eventQHndl = new JXIOEventQueueHandler(500);


		JXIOServerManager man = new MySesManager(eventQHndl, url);
		
		eventQHndl.run(); 
	}
		
}
