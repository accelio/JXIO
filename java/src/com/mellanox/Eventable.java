package com.mellanox;


public interface Eventable {
	public void onEvent (int eventType,Event ev);
/*
	enum eventType {
		sessionError, msgError, sessionEstablished, msgRecieved,
	    newSession 
	}
*/
	public boolean close();
	
	public long getId();

}
