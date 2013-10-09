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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Msg {

	private static final Log LOG= LogFactory.getLog(Msg.class.getCanonicalName());
	private long refToCObject;
	private ClientSession clientSession;
	private MsgPool msgPool; //reference to MsgPool holding this buffer
	private ByteBuffer in, out;

	Msg(ByteBuffer buffer, int inSize, int outSize, long id, MsgPool msgPool){
		in = createSubBuffer(0, inSize, buffer);
		out = createSubBuffer(inSize, buffer.capacity(), buffer);
		this.msgPool = msgPool;
		this.refToCObject = id;
		LOG.debug("IN: capacity is " + in.capacity() + " limit " + in.limit()+ " position "+ in.position()+ " remaining is "+ in.remaining());
		LOG.debug("OUT: capacity is " + out.capacity() + " limit " + out.limit()+ " position "+ out.position()+ " remaining is "+ out.remaining());
	}
	public void returnToParentPool() {
		msgPool.releaseMsg(this);
	}

	public ByteBuffer getIn() {
		return in;
	}

	public ByteBuffer getOut() {
		return out;
	}

	MsgPool getParentPool() {
		return msgPool;
	}

	void setClientSession(ClientSession clientSession) {
		this.clientSession = clientSession;
	}

	ClientSession getClientSession() {
		return clientSession;
	}

	long getId() {
		return refToCObject;
	}

	private ByteBuffer createSubBuffer(int position, int limit, ByteBuffer buf){
		ByteBuffer sub;
		buf.position(position);
		buf.limit(limit);
		sub = buf.slice();
		sub.position(0);
		return sub;
	}
}
