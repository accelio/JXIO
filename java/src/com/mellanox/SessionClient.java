package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;


public abstract class SessionClient implements Eventable{// extends SessionBase {
	
	private long id = 0;
	private long ptrEventQueue = 0;
	protected String url;
	protected int port;
	
	protected long conID = 0;
	
	abstract public void onReplyCallback();
	abstract public void onSessionEstablished();
	
	private static JXLog logger = JXLog.getLog(SessionClient.class.getCanonicalName());
	
	public SessionClient(long eqhPtr, String url, int port){
		//call omx create session
		this.ptrEventQueue = eqhPtr;
		this.url = url;
		this.port = port;
		
		long [] ar;
		
		ar = JXBridge.startClientSession(url, port, ptrEventQueue);
		
		id = ar[0];
		logger.log(Level.INFO, "katya3 id is "+id);
		this.conID = ar[1];
	}
	
	
	public void onEvent(int eventType, ByteBuffer buf){
		
		switch (eventType){
		
		
		case 0: //session error event
			logger.log(Level.INFO, "received session error event");
			int errorType = buf.getInt();
			int reason = buf.getInt();
			String s = JXBridge.getError(reason);
			this.onSessionErrorCallback(errorType, s);
			break;
			
		case 1: //msg error
			logger.log(Level.INFO, "received msg error event");
			this.onMsgErrorCallback();
			break;

		case 2: //session established
			logger.log(Level.INFO, "received session established event");
			this.onSessionEstablished();
			break;
			
		case 3: //on reply
			logger.log(Level.INFO, "received msg event");
			this.onReplyCallback();
			break;
			
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
		}
		
	}
	
	
	abstract public void onSessionErrorCallback(int session_event, String reason );
	abstract public void onMsgErrorCallback();

	public long getId(){ return id;}
	
	public boolean close (){
//		eventQHandler.removeSesssion (this); //TODO: fix this
		return JXBridge.closeConnectionClient(conID);		
	}
	
	public boolean closeSession(){
		return JXBridge.closeSessionClient(id);
	}
	
}
