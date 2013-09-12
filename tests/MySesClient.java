import java.util.logging.Level;
import com.mellanox.*;

	public class MySesClient extends JXIOClientSession{
		
		private static JXIOLog logger = JXIOLog.getLog(MySesClient.class.getCanonicalName());
		
		public MySesClient(JXIOEventQueueHandler eqh, String uri){
			super (eqh, uri);	
		}
		
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
	
		public void onReply(){	
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

