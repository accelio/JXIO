package com.mellanox.jxio.tests.random.storyrunner;

public class Process {
	
	private int id;
	private String location;
	private int numEQHs;
	private int timeout;
	
	public Process(int id, String location, int numEQHs, int timeout) {
	    this.id = id;
	    this.location = location;
	    this.numEQHs = numEQHs;
	    this.timeout = timeout;
    }
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public int getNumEQHs() {
		return numEQHs;
	}
	public void setNumEQHs(int numEQHs) {
		this.numEQHs = numEQHs;
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}
