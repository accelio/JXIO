package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;


public abstract class SessionServer implements Eventable {//extends SessionBase{
	
	private long id = 0;
	private long ptrEventQueue = 0;
	protected String url;
	protected int port;
	
	
	abstract public void onRequestCallback();
	
	private static JXLog logger = JXLog.getLog(SessionServer.class.getCanonicalName());

	protected SessionServer(long eqh, String url, int port) {
		this.ptrEventQueue = eqh;
		this.url = url;
		this.port = port;
		logger.log(Level.INFO, "uri inside SessionServer is "+url);
		this.id = JXBridge.startServer(url, port, ptrEventQueue);
		// TODO Auto-generated constructor stub
	}
	
	public long getId(){ return id;}
	
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

		case 3: //on request
			logger.log(Level.INFO, "received msg event");
			this.onRequestCallback();
			break;
			
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);	
		}
		
	}
	
	public void close(){
		JXBridge.stopServer(id);
	}
	
	
	abstract public void onSessionErrorCallback(int session_event, String reason );
	abstract public void onMsgErrorCallback();

}
