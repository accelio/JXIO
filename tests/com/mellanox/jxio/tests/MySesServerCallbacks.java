package com.mellanox.jxio.tests;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.ServerSession;

class MySesServerCallbacks implements ServerSession.Callbacks {

	private final static Log LOG = LogFactory.getLog(MySesServerCallbacks.class.getCanonicalName());
	private ServerSession serverSession;
	public void onRequest(Msg msg) {
		LOG.info("got a request! Bring the champagne!!!!!");
		LOG.info("msg is "+msg);
		int num = msg.getIn().getInt();
		LOG.info("got "+num);
		msg.getOut().putInt(num);
		serverSession.sendResponce(msg);
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
		LOG.error("GOT EVENT " + event + "because of " + reason);
	}

	public void onMsgError() {
		LOG.info("onMsgErrorCallback");

	}
}
