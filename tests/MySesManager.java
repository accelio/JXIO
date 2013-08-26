import java.util.logging.Level;

import com.mellanox.*;

public class MySesManager extends ServerManager{

	private static JXLog logger = JXLog.getLog(MySesManager.class.getCanonicalName());
	
	public MySesManager(EventQueueHandler eqh, String url) {
		super(eqh, url);
	}
	
	public void onSession(long ptrSes, String uri, String srcIP) {
		//has some logic on which port the new session will listen
		int port = 1235;
		logger.log(Level.INFO, "MySesManager.onSession uri is "+uri);
		
		MyEQH eventQHndl = new MyEQH (10000);	
		MySesServer ses = new MySesServer(eventQHndl, uri);
		eventQHndl.addEventable (ses);
		forward(ses, ptrSes);
		
		Thread t = new Thread (eventQHndl);
		t.start();

		
//		eventQHndl.runEventLoop2(1, 0);
//		eventQHndl.runEventLoop2(1, 0);
//		eventQHndl.runEventLoop2(1, 0);
		
		
	}

	public void onSessionError(int session_event, String reason) {
		String event;
		switch (session_event){
		case 0:
			event = "SESSION_REJECT";
			this.close(); // Added
			break;
		case 1:
			event = "SESSION_TEARDOWN";
			this.close(); // Added
			break;
		case 2:
			event = "CONNECTION_CLOSED";
			this.close(); // Added
			break;
		case 3:
			event = "CONNECTION_ERROR";
			this.close(); // Added
			break;
		case 4:
			event = "SESSION_ERROR";
			this.close(); // Added
			break;
		default:
			event = "UNKNOWN";
			this.close(); // Added
			break;
		}
		logger.log(Level.SEVERE, "GOT EVENT " + event + "because of " + reason);
		
	}


}
