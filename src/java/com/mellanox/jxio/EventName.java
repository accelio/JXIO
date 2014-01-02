package com.mellanox.jxio;

public enum EventName {
	SESSION_REJECT(0), SESSION_TEARDOWN(1), NEW_CONNECTION_EVENT (2), SESSION_CONNECTION_TEARDOWN(3),
	SESSION_CONNECTION_CLOSED(4), SESSION_CONNECTION_DISCONNECTED(5), SESSION_CONNECTION_ERROR(6), SESSION_ERROR(7);
	
	private int index;
	
	private EventName(int i) {
		   index = i;
	}
	public int getIndex(){return index;}
	
	private static EventName [] allEvents = values();
	public static EventName getEventByIndex(int index){
		return allEvents[index];
	}
}