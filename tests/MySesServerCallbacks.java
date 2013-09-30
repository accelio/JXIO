

import java.util.logging.Level;

import com.mellanox.jxio.Log;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.ServerSession;

class MySesServerCallbacks implements ServerSession.Callbacks {

	private static Log logger = Log.getLog(MySesServerCallbacks.class.getCanonicalName());

	public void onRequest(Msg msg) {
		logger.log(Level.INFO, "got a request! Bring the champagne!!!!!");
	}

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

	public void onMsgError() {
		logger.log(Level.INFO, "onMsgErrorCallback");

	}
}
