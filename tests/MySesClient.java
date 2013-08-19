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
		
		
		//TODO: to have a mechanism of default implentation for each type of event
		public void onSessionErrorCallback(int session_event, String reason ){
			String event;
			switch (session_event){
			case 0:
				event = "SESSION_REJECT";
				break;
			case 1:
				event = "SESSION_TEARDOWN";
				closeSession(); //through the bridge calls d-tor of cjxsession(which does nothing)
				break;
			case 2:
				event = "CONNECTION_CLOSED";
//				System.out.println("GOT EVENT CONNECTION_CLOSED");
				//this is fine - connection closed by choice
				//there are two options: close session or reopen it
				break;
			case 3:
				event = "CONNECTION_ERROR";
				////disconnect session
//				System.out.println("GOT EVENT CONNECTION_ERROR");
				closeConnection(); //through the bridge calls connection close
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
			
		}
	
		public void onReplyCallback(){	
			logger.log(Level.INFO, "got a message! Bring the champagne!!!!!");
		}
		

		
	}

