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
		eventQHndl.addEventable (ses);
		forward(ses, ptrSes);
		
		Thread t = new Thread (eventQHndl);
		t.start();

		
//		eventQHndl.runEventLoop2(1, 0);
//		eventQHndl.runEventLoop2(1, 0);
//		eventQHndl.runEventLoop2(1, 0);
		
		
	}

	@Override
	public void onSessionError(int session_event, String reason) {
		String event;
		switch (session_event){
		case 0:
			event = "SESSION_REJECT";
			break;
		case 1:
			event = "SESSION_TEARDOWN";
			break;
		case 2:
			event = "CONNECTION_CLOSED";
			break;
		case 3:
			event = "CONNECTION_ERROR";
			break;
		case 4:
			event = "SESSION_ERROR";
			break;
		default:
			event = "UNKNOWN";
			break;
		}
		logger.log(Level.SEVERE, "GOT EVENT " + event + "because of " + reason);
		
	}


}
