package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;


public abstract class SessionClient extends SessionBase {
	
	abstract public void onReplyCallback();
	abstract public void onSessionEstablished();
	
	private static JXLog logger = JXLog.getLog(SessionClient.class.getCanonicalName());
	
	public SessionClient(long eqhPtr, String url, int port){
		super (eqhPtr, url, port, 0);	
	}
	
	
	public void onEvent(int eventType, ByteBuffer buf){
		switch (eventType){
		case 2: //session established
			logger.log(Level.INFO, "received session established event");
			this.onSessionEstablished();
			break;
			
		case 4: //on reply
			logger.log(Level.INFO, "received msg event");
			this.onReplyCallback();
			break;
			
		default:
			super.onEvent(eventType, buf);
		}
		
	}
	
	
	
	//TODO: temp implementation - just to test the control path
	int sendRequest(String s){
		return 0;
	}
	

}
