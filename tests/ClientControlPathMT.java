import com.mellanox.*;



public class ClientControlPathMT {
private static JXLog logger = JXLog.getLog(ClientControlPathSimpleTest.class.getCanonicalName());

	
	public static void main(String[] args){
		
		String url = args[0];
		String port = args[1];
		String combined_url = "rdma://"+url+":"+port;
		MySesClient ses;
		MyEQH eventQHndl;
		
		int num_times = 1;
		
		eventQHndl = new MyEQH (1000);
		
		for (int i=0; i<num_times; i++){
			
			ses = new MySesClient(eventQHndl, url,Integer.parseInt(port));
			eventQHndl.addEventHandler (ses);
			Thread t = new Thread (eventQHndl);
			t.start();
			
			
			Thread.currentThread();
			try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
/*				
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			*/
			ses.close();
			
		}
		eventQHndl.close();

	}
}
