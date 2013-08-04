package com.mellanox;


public abstract class SessionClient extends SessionBase {
	
	abstract public void onReplyCallback();
	abstract public void onSessionEstablished();
	
	public SessionClient(EventQueueHandler eqh, String uri){
		super (eqh, uri);	
	}
	
	
	//TODO: temp implementation - just to test the control path
	int sendRequest(String s){
		return 0;
	}
	

}
