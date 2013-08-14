import java.util.logging.Level;

import com.mellanox.*;


public class MySesServer extends SessionServer {
	private static JXLog logger = JXLog.getLog(MySesServer.class.getCanonicalName());
	EventQueueHandler eqh = null;

	public MySesServer(EventQueueHandler eqh, String uri, int port){
		super (eqh.getID(), uri, port);
		this.eqh = eqh;
	}

	@Override
	public void onRequestCallback() {
		logger.log(Level.INFO, "got a request! Bring the champagne!!!!!");
	}

	@Override
	public void onSessionErrorCallback(int session_event, String reason) {
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
		

	@Override
	public void onMsgErrorCallback() {
		logger.log(Level.INFO, "onMsgErrorCallback");
		
	}

	

}
