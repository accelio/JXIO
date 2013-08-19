package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;


public abstract class SessionClient implements Eventable{
	
	private long id = 0;
	private long ptrEventQueue = 0;
	protected String url;
	protected int port;

	
	abstract public void onReplyCallback();
	abstract public void onSessionEstablished();
	abstract public void onSessionErrorCallback(int session_event, String reason );
	abstract public void onMsgErrorCallback();

	
	private static JXLog logger = JXLog.getLog(SessionClient.class.getCanonicalName());
	
	public SessionClient(long eqhPtr, String url, int port){
		//call omx create session
		this.ptrEventQueue = eqhPtr;
		this.url = url;
		this.port = port;
		
		this.id = JXBridge.startClientSession(url, port, ptrEventQueue);
		logger.log(Level.INFO, "id is "+id);

	}
	
	
	public void onEvent(int eventType, ByteBuffer buf){
		
		switch (eventType){

		case 0: //session error event
//			logger.log(Level.INFO, "received session error event");
			System.out.println("received session error event");
			int errorType = buf.getInt();
			int reason = buf.getInt();
			String s = JXBridge.getError(reason);
			this.onSessionErrorCallback(errorType, s);
			break;
			
		case 1: //msg error
//			logger.log(Level.INFO, "received msg error event");
			System.out.println("received msg error event");
			this.onMsgErrorCallback();
			break;

		case 2: //session established
//			logger.log(Level.INFO, "received session established event");
			System.out.println("received session established event");
			this.onSessionEstablished();
			break;
			
		case 3: //on reply
//			logger.log(Level.INFO, "received msg event");
			System.out.println("received msg event");
			this.onReplyCallback();
			break;
			
		default:
//			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
			System.out.println("received an unknown event "+ eventType);
		}
		
	}
	
//	public boolean closeSession(){
//		return JXBridge.closeSessionClient(id);
//	}
	
	public long getId(){ return id;}
	
	
	public boolean close (){
//		eventQHandler.removeSesssion (this); //TODO: fix this
		return JXBridge.closeConnectionClient(id);		
	}
	
}
