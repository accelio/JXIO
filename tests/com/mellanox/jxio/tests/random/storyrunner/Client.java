package com.mellanox.jxio.tests.random.storyrunner;

public class Client {
	
	private int id;
	private int process;
	private int duration;
	private int batch;
	private int server;
	private int startDelay;
	private int tps;
	
	public Client(int id, int process, int duration, int batch, int server, int startDelay, int tps) {
	    this.id = id;
	    this.process = process;
	    this.duration = duration;
	    this.batch = batch;
	    this.server = server;
	    this.startDelay = startDelay;
	    this.tps = tps;
    }
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getProcess() {
		return process;
	}
	public void setProcess(int process) {
		this.process = process;
	}
	public int getDuration() {
		return duration;
	}
	public void setDuration(int duration) {
		this.duration = duration;
	}
	public int getBatch() {
		return batch;
	}
	public void setBatch(int batch) {
		this.batch = batch;
	}
	public int getServer() {
		return server;
	}
	public void setServer(int server) {
		this.server = server;
	}
	public int getStartDelay() {
		return startDelay;
	}
	public void setStartDelay(int startDelay) {
		this.startDelay = startDelay;
	}
	public int getTps() {
		return tps;
	}
	public void setTps(int tps) {
		this.tps = tps;
	}

}
