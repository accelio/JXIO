package com.mellanox;


import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.mellanox.JXBridge;


public abstract class SessionBase {
	
//	private EventQueueHandler eventQHandler = null;
	private long ptrEventQueue = 0;
	private long id = 0;
	private Connection con = null;
	protected int type;
	
	protected String url;
	protected int port;
	
	private static JXLog logger = JXLog.getLog(SessionBase.class.getCanonicalName());
	
	protected SessionBase (long eqh, String url, int port, int type){
		//call omx create session
		this.ptrEventQueue = eqh;
		this.url = url;
		this.port = port;
		
		long [] ar;
		
		ar = JXBridge.startSession(url, port, ptrEventQueue, type);
		
		id = ar[0];
		con = new Connection( ar[1]);

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
		
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
		}
		
	}
	
	
	abstract public void onSessionErrorCallback(int session_event, String reason );
	abstract public void onMsgErrorCallback();
	
	public boolean close (){
//		eventQHandler.removeSesssion (this); TODO: to fix this (base/clientSession issue)
		return JXBridge.closeSesCon(id, con.id);		
	}

	
	class Connection{
		long id;
		
		Connection(long id){
			this.id = id;}
	}
}
