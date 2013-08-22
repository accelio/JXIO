package com.mellanox;

public class EventSession extends Event{
	int errorType;
	String reason;
	EventSession(int error, String s){
		this.errorType = error;
		this.reason = s;
	}
}
