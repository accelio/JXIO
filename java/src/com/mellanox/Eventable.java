package com.mellanox;

import java.nio.ByteBuffer;

public interface Eventable {
	public void onEvent (int eventType, ByteBuffer buffer);
/*
	enum eventType {
		sessionError, msgError, sessionEstablished, msgRecieved,
	    newSession 
	}
*/
	public boolean close();

}
