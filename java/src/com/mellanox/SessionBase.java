package com.mellanox;


import com.mellanox.JXBridge;


public abstract class SessionBase {
	
	private EventQueueHandler eventQHandler = null;
	private long id = 0;
	private Connection con = null;
	
	protected String uri;
	
	
	protected SessionBase (EventQueueHandler eqh, String uri){
		//call omx create session
		this.eventQHandler = eqh;
		this.uri = uri;
		
	}
	
	abstract public void onSessionErrorCallback(int session_event, String reason );
	abstract public void onMsgErrorCallback();
	
	public boolean close (){
//		eventQHandler.removeSesssion (this); TODO: to fix this (base/clientSession issue)
		return JXBridge.closeSesCon(id, con.id);
		
	}

	
	class Connection{
		long id;
		
	}
}
