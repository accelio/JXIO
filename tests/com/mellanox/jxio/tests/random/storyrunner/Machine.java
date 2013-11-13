package com.mellanox.jxio.tests.random.storyrunner;

public class Machine {
	
	private int id;
	private String manageInterface;
	private String address;
	private String name;
	private String type;
	
	public Machine(int id, String manageInterface, String address, String name, String type) {
		this.id = id;
		this.manageInterface = manageInterface;
		this.address = address;
		this.name = name;
		this.type = type;
    }
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getManageInterface() {
		return manageInterface;
	}
	public void setManageInterface(String manageInterface) {
		this.manageInterface = manageInterface;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}
