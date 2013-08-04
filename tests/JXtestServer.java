
import com.mellanox.JXMsgPool;
import com.mellanox.ServerJXSession;
import java.net.InetAddress;

public class JXtestServer {

	static private final int NUM_OF_MSG = 1000 ; // 
	static private final int SIZE_OF_MSG = 512 ; // size in KB
	
	public static void main(String[] args){
		String port = args[0];
		JXMsgPool pool = new JXMsgPool(NUM_OF_MSG, SIZE_OF_MSG);
		ServerJXSession s = new ServerJXSession(pool);
		// starts a server session
		String hostname = null ;
		try{
			hostname = java.net.InetAddress.getLocalHost().getHostName();
			s.startSession(hostname, Integer.parseInt(port));
		}catch (Exception e){}
	}
}
