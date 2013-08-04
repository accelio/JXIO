package com.mellanox;


public class JXMsg {
	
	private int offset;
	private int msg_size;

	
	protected JXMsg(int _offset ,int _msg_size){
		offset = _offset;
		msg_size = _msg_size;
	}
	
	public int getOffset(){
		return offset;
	}
	
	public int getSize(){
		return msg_size;
	}

}
