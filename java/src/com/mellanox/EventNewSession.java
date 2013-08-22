package com.mellanox;

class EventNewSession extends Event{
	long ptrSes;
	String uri;
	String srcIP;
	EventNewSession (long ptr, String uri, String ip){
		this.ptrSes = ptr;
		this.uri = uri;
		this.srcIP = ip;
	}
}
