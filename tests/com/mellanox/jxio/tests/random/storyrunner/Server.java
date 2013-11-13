package com.mellanox.jxio.tests.random.storyrunner;

public class Server {
	
	private int id;
	private int process;
	private int duration;
	private int maxSSPerSM;
	private int delay;
	private int startDelay;
	private int tps;

	public Server(int id, int process, int duration, int maxSSPerSM, int delay, int startDelay, int tps) {
	    this.id = id;
	    this.process = process;
	    this.duration = duration;
	    this.maxSSPerSM = maxSSPerSM;
	    this.delay = delay;
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
	public int getMaxSSPerSM() {
		return maxSSPerSM;
	}
	public void setMaxSSPerSM(int maxSSPerSM) {
		this.maxSSPerSM = maxSSPerSM;
	}
	public int getDelay() {
		return delay;
	}
	public void setDelay(int delay) {
		this.delay = delay;
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
