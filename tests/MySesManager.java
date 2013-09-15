import java.util.logging.Level;

import com.mellanox.jxio.*;

public class MySesManager extends ServerManager {

	private static Log logger = Log.getLog(MySesManager.class.getCanonicalName());
	
	public MySesManager(EventQueueHandler eqh, String url) {
		super(eqh, url);
	}
	
	
	public void onSession(long ptrSes, String uriSrc, String srcIP) {		
			EventQueueHandler eventQHndl = new EventQueueHandler();	
		MySesServer ses = new MySesServer(eventQHndl, super.getUrlForServer());
		forward(ses, ptrSes);
		
		Thread t = new Thread (eventQHndl);
		t.start();	
		
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
