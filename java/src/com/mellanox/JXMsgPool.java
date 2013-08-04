package com.mellanox;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.nio.ByteBuffer;	
import java.util.logging.Level;

import com.mellanox.JXBridge;


public class JXMsgPool {
	
	private static JXLog logger = JXLog.getLog(JXMsgPool.class.getCanonicalName());
	
	// Direct buffer to the registered memory
	ByteBuffer dbuff = null;
	
	// number of Msgs
	private int num_of_msgs ;
	
	// msg size
	private int msg_size ;
	
	// list of free msgs
	private List<JXMsg> free_msg_list ;
	
	
	// Ctore, allocates & register direct buffer in C code.
	public JXMsgPool(int _num_of_msgs, int _msg_size){
		num_of_msgs = _num_of_msgs;
		msg_size = _msg_size;
		dbuff = JXBridge.createMsgPool(num_of_msgs, msg_size);
		if(dbuff==null){
			logger.log(Level.SEVERE, "Msg pool could not be created");
		}
		else{
			// create the list of msgs to be used to send (synchronyzed)
			free_msg_list = Collections.synchronizedList(new LinkedList<JXMsg>());
			for(int i=0; i<num_of_msgs; i++){
				free_msg_list.add(new JXMsg((i*msg_size), msg_size));
			}
		}
	}
	
	
	// mark one msg as 'not free' and return the proper offset in the direct buffer
	public int getMsg(){
		while(!free_msg_list.isEmpty()){
			return free_msg_list.remove(0).getOffset();
			//need to add timer for not getting a free msg
		}
		return -1;
	}
	
	
	// return msg to pool (abstraction of de-allocating Msg in C code)
	public void returnMsgToPool(JXMsg m){
		free_msg_list.add(m);
	}
	
}


