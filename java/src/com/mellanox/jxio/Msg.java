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
import java.util.logging.Level;

public class Msg {
	
   
    private static Log logger = Log.getLog(Msg.class.getCanonicalName());
    private long refToCObject;
    private ClientSession clientSession;
    private MsgPool msgPool; //reference to MsgPool holding this buffer
    private ByteBuffer in, out;
    
    Msg(ByteBuffer buffer, int inSize, int outSize, long id, MsgPool msgPool){
	createSubBuffer(0, inSize, in, buffer);
	in.position(0);
	createSubBuffer(inSize, buffer.capacity(), out, buffer);
	
	this.msgPool = msgPool;
	this.refToCObject = id;
	
	
	logger.log(Level.INFO, "IN: capacity is " + in.capacity() + " limit " + in.limit()+ " position "+ in.position()+ " remaining is "+ in.remaining());
	logger.log(Level.INFO, "OUT: capacity is " + out.capacity() + " limit " + out.limit()+ " position "+ out.position()+ " remaining is "+ out.remaining());

    }
    
   

    public ByteBuffer getIn() {
        return in;
    }
    
    MsgPool getParentPool(){
	return msgPool;
    }

    public ByteBuffer getOut() {
        return out;
    }

    void setClientSession(ClientSession clientSession) {
	this.clientSession = clientSession;
    }

    ClientSession getClientSession() {
	return clientSession;
    }

    public long getId() {
	return refToCObject;
    }
    
    public void releaseMsg(){
	msgPool.releaseMsg(this);
    }
    
    private void createSubBuffer(int position, int limit, ByteBuffer sub, ByteBuffer buf){
	buf.position(position);
	buf.limit(limit);
	sub = buf.slice();
    }
}
	
	

	
	
	
	
	