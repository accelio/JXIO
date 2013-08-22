import java.util.logging.Level;
import com.mellanox.*;

	public class MySesClient extends SessionClient{
		
		private static JXLog logger = JXLog.getLog(MySesClient.class.getCanonicalName());
		
		public MySesClient(EventQueueHandler eqh, String uri, int port){
			super (eqh, uri);	
		}
		
		public void onMsgErrorCallback(){	
			logger.log(Level.INFO, "[SUCCESS] onMsgErrorCallback");
			System.out.println("!!!!!!! onMsgErrorCallback !!!!!!!");
			this.close();
		}
		
		public void onSessionEstablished(){
			logger.log(Level.INFO, "[SUCCESS] Session Established! Bring the champagne!");
		}

		public void onSessionErrorCallback(int session_event, String reason ){
			
			String event;
			switch (session_event)
			{
			case 0:
				event = "SESSION_REJECT";
				break;
			case 1:
				event = "SESSION_TEARDOWN";
				this.close();
				break;
			case 2:
				event = "CONNECTION_CLOSED";
				// This is fine - connection closed by choice
				// there are two options: close session or reopen it
				break;
			case 3:
				event = "CONNECTION_ERROR";
				this.close(); //through the bridge calls connection close
				break;
			case 4:
				event = "SESSION_ERROR";
				break;
			default:
				event = "UNKNOWN_EVENT";
				break;
			}
			
			logger.log(Level.SEVERE, "[EVENT] GOT EVENT " + event + " because of " + reason);
			System.out.println("[EVENT] GOT EVENT " + event + " because of " + reason);
			
		}
	
		public void onReplyCallback(){	
			logger.log(Level.INFO, "[SUCCESS] Got a message! Bring the champagne!");
		}

		public boolean close(){
			if (super.close()){
				logger.log(Level.INFO, "[SUCCESS] Session client successfully closed!" );
				return true;
			} else {
				logger.log(Level.INFO, "[ERROR] Session client failed to close!");
				return false;
			}
		}
	}

