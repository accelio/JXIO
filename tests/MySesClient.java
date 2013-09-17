import java.util.logging.Level;

import com.mellanox.jxio.*;

	public class MySesClient {
		
		private static Log logger = Log.getLog(MySesClient.class.getCanonicalName());
		
	    EventQueueHandler eqh = null;
	    ClientSession client;
	    ClientSessionCallbacks callbacks;
		
		
		
		public MySesClient(EventQueueHandler eqh, String uri){
		this.callbacks = new MySesClientCallbacks();
		this.client = new ClientSession (eqh, uri, callbacks);
		this.eqh = eqh;	
	    }	    
	    
	    
	    public boolean close(){
		if (this.client.close()){
		    logger.log(Level.INFO, "[SUCCESS] Session client successfully closed!" );
		    return true;
		} else {
		    logger.log(Level.INFO, "[ERROR] Session client failed to close!");
		    return false;
		}
	    }
		
	    
	    
	    class MySesClientCallbacks implements ClientSessionCallbacks {
		
		public void onMsgError(){	
			logger.log(Level.INFO, "onMsgErrorCallback");
		}
		
		public void onSessionEstablished(){
			logger.log(Level.INFO, "[SUCCESS] Session Established! Bring the champagne!");
		}

		public void onSessionError(int session_event, String reason ){
			
			String event;
			switch (session_event)
			{
			case 0:
				event = "SESSION_REJECT";
				break;
			case 1:
				event = "SESSION_TEARDOWN";
				break;
			case 2:
				event = "CONNECTION_CLOSED";
				// This is fine - connection closed by choice
				// there are two options: close session or reopen it
				break;
			case 3:
				event = "CONNECTION_ERROR";
//				this.close(); //through the bridge calls connection close
				break;
			case 4:
				event = "SESSION_ERROR";
				break;
			default:
				event = "UNKNOWN_EVENT";
				break;
			}
			
			logger.log(Level.SEVERE, "[EVENT] GOT EVENT " + event + " because of " + reason);
			
		}
	
		public void onReply(Msg msg){	
			logger.log(Level.INFO, "[SUCCESS] Got a message! Bring the champagne!");
		}

		
	}
	}
