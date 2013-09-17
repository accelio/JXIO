import java.util.logging.Level;

import com.mellanox.jxio.*;

public class MySesManager  {

	private static Log logger = Log.getLog(MySesManager.class.getCanonicalName());
	EventQueueHandler eqh = null;
	ServerManager serverManager;
	ServerManagerCallbacks callbacks;
	
	public MySesManager(EventQueueHandler eqh, String url) {
	    this.callbacks = new MySesManagerCallbacks();
	    this.serverManager = new ServerManager (eqh, url, callbacks);
	    this.eqh = eqh;	
	}
	
	public void close(){
	    serverManager.close();
	}
	
	class MySesManagerCallbacks implements  ServerManagerCallbacks {
	
	
	public void onSession(long ptrSes, String uriSrc, String srcIP) {		
			EventQueueHandler eventQHndl = new EventQueueHandler();	
		ServerSession ses = new ServerSession (eventQHndl, serverManager.getUrlForServer(), new MySesServerCallbacks());
		serverManager.forward(ses, ptrSes);
		
		Thread t = new Thread (eventQHndl);
		t.start();	
		
	}

	public void onSessionError(int session_event, String reason) {
		String event;
		switch (session_event){
		case 0:
			event = "SESSION_REJECT";
			serverManager.close(); // Added
			break;
		case 1:
			event = "SESSION_TEARDOWN";
			serverManager.close(); // Added
			break;
		case 2:
			event = "CONNECTION_CLOSED";
			serverManager.close(); // Added
			break;
		case 3:
			event = "CONNECTION_ERROR";
			serverManager.close(); // Added
			break;
		case 4:
			event = "SESSION_ERROR";
			serverManager.close(); // Added
			break;
		default:
			event = "UNKNOWN";
			serverManager.close(); // Added
			break;
		}
		logger.log(Level.SEVERE, "GOT EVENT " + event + "because of " + reason);
	}
	}
}
