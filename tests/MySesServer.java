import java.util.logging.Level;

import com.mellanox.jxio.*;


public class MySesServer {
	private static Log logger = Log.getLog(MySesServer.class.getCanonicalName());
	EventQueueHandler eqh = null;
    	ServerSession server;
    	ServerSessionCallbacks callbacks;
	public MySesServer(EventQueueHandler eqh, String uri){
    	    	this.callbacks = new MySesServerCallbacks();
		this.server = new ServerSession (eqh, uri, callbacks);
		this.eqh = eqh;
	}

	
	

	

}
