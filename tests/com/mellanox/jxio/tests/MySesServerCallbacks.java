package com.mellanox.jxio.tests;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.ServerSession;
import com.mellanox.jxio.EventName;

class MySesServerCallbacks implements ServerSession.Callbacks {

	private final static Log LOG = LogFactory.getLog(MySesServerCallbacks.class.getCanonicalName());
	ServerSession serverSession;
	public void onRequest(Msg msg) {
		LOG.info("got a request! Bring the champagne!!!!!");
		LOG.info("msg is "+msg);
		int num = msg.getIn().getInt();
		LOG.info("got "+num);
		msg.getOut().putInt(num);
		serverSession.sendResponce(msg);
	}

    public void onSessionEvent(EventName session_event, String reason) {
		LOG.error("GOT EVENT " + session_event.toString() + "because of " + reason);
	}

	public void onMsgError() {
		LOG.info("onMsgErrorCallback");

	}
}
