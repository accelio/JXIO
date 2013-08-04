package com.mellanox;


import java.util.logging.Level;

import com.mellanox.JXBridge;


public abstract class SessionBase {
	
	private EventQueueHandler eventQHandler = null;
	private long id = 0;
	private Connection con = null;
	
	protected String url;
	protected int port;
	
	private static JXLog logger = JXLog.getLog(SessionBase.class.getCanonicalName());
	
	protected SessionBase (EventQueueHandler eqh, String url, int port){
		//call omx create session
		this.eventQHandler = eqh;
		this.url = url;
		this.port = port;
		
		long [] ar;
		
		ar = JXBridge.startClientSession(url, port, eqh.getID());
		
		logger.log(Level.INFO, "the array that i got1!" + ar[0]);
		logger.log(Level.INFO, "the array that i got1!" + ar[1]);
		
		id = ar[0];
		con = new Connection( ar[1]);

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
