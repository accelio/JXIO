/*
** Copyright (C) 2013 Mellanox Technologies
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at:
**
** http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
** either express or implied. See the License for the specific language
** governing permissions and  limitations under the License.
**
*/
package com.mellanox.jxio;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.mellanox.jxio.impl.Bridge;

public class MsgPool {
	
    private static Log logger = Log.getLog(MsgPool.class.getCanonicalName());
    ByteBuffer buffer;
    List<Msg> listMsg = new ArrayList<Msg>();
    
    public MsgPool(int count, int inSize, int outSize){
	Bridge.createMsgPool(count, inSize, outSize);
	int ptrMsg [] = new int [count]; //TODO : to recieve from c properly
	int msgBufferSize = inSize + outSize;
	
	for (int i=0; i<count; i++){
	    ByteBuffer partialBuffer = buffer.slice();
	    partialBuffer.limit(msgBufferSize * (i+1));
	    partialBuffer.position(msgBufferSize * i);
	    Msg m = new Msg(partialBuffer, inSize, outSize, ptrMsg[i]);
	    listMsg.add(m);
	}
    }
    
    public Msg getMsg(){
	if (!listMsg.isEmpty()){
	    logger.log(Level.SEVERE, "there are no more messages in pool");
	    return null;
	    
	}
	Msg msg = listMsg.get(0);
	listMsg.remove(0);
	return msg;
    }
    
    public void releaseMsgToPool(Msg msg){
	listMsg.add(msg);
    }
}
	
	

	
	
	
	
	