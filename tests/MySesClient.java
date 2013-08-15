import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import com.mellanox.*;

	public class MySesClient extends SessionClient{
		private static JXLog logger = JXLog.getLog(MySesClient.class.getCanonicalName());
		
		
		public MySesClient(EventQueueHandler eqh, String uri, int port){
			super (eqh.getID(), uri, port);	
		}
		
		
		public void onMsgErrorCallback(){	
			logger.log(Level.INFO, "onMsgErrorCallback");

		}
		
		public void onSessionEstablished(){
			logger.log(Level.INFO, "session established! Bring the champagne!!!!!");
		}
		
		public void onSessionErrorCallback(int session_event, String reason ){
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
			System.out.println("GOT EVENT " + event + "because of " + reason);
			//there are two options: close session or reopen it
			closeSession();
			
			
		}
	
		public void onReplyCallback(){	
			logger.log(Level.INFO, "got a message! Bring the champagne!!!!!");
		}
		

		
	}

