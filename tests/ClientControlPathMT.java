import com.mellanox.jxio.*;

public class ClientControlPathMT {
private static Log logger = Log.getLog(ClientControlPathSimpleTest.class.getCanonicalName());

	
	public static void main(String[] args) {
		
		String url = args[0];
		String port = args[1];
		String combined_url = "rdma://"+url+":"+port;
		MySesClient ses;
		EventQueueHandler eventQHndl;
		
		int num_times = 1;
		
		eventQHndl = new EventQueueHandler();
		
		for (int i=0; i<num_times; i++){
			
			ses = new MySesClient(eventQHndl, url);
//			eventQHndl.addEventable (ses);
			Thread t = new Thread (eventQHndl);
			t.start();
			
			
			Thread.currentThread();
			try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			System.out.println("***********************after sleep1");
			
			try {
			    t.sleep(5000);
			    System.out.println("***********************after sleep2");
			} catch (InterruptedException e1) {
			    // TODO Auto-generated catch block
			    e1.printStackTrace();
			}
			ses.close();
			System.out.println("***********************here");
			try {
			    	eventQHndl.stopEventLoop();
				t.join();
				System.out.println("***********************after join");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		eventQHndl.close();
	}
}
