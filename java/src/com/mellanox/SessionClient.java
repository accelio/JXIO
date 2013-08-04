package com.mellanox;


public abstract class SessionClient extends SessionBase {
	
	abstract public void onReplyCallback();
	abstract public void onSessionEstablished();
	
	public SessionClient(EventQueueHandler eqh, String url, int port){
		super (eqh, url, port);	
	}
	
	
	//TODO: temp implementation - just to test the control path
	int sendRequest(String s){
		return 0;
	}
	

}
