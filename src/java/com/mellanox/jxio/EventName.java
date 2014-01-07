package com.mellanox.jxio;

public enum EventName {
	SESSION_REJECT(0), SESSION_CLOSED(1), SESSION_ERROR(7);
	
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