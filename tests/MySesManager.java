import java.util.logging.Level;

import com.mellanox.*;

public class MySesManager extends SessionManager{

	private static JXLog logger = JXLog.getLog(MySesManager.class.getCanonicalName());
	
	public MySesManager(String url, int port) {
		super(url, port);
	}

	@Override
	public void onSession(long ptrSes, String uri, String srcIP) {
		//has some logic on which port the new session will listen
		int port = 1235;
		logger.log(Level.INFO, "MySesManager.onSession uri is "+uri);
		
		MyEQH eventQHndl = new MyEQH (10000);	
		MySesServer ses = new MySesServer(eventQHndl, uri, port);
		forward(ses, ptrSes);
//		Thread t = new Thread (eventQHndl);
//		t.start();

		
//		eventQHndl.runEventLoop(1, 0);
//		eventQHndl.runEventLoop(1, 0);
		
		
	}


}
