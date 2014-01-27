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

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.EventQueueHandler.Eventable;

/**
 * Msg is the object that represents a message received from or to be sent to another
 * peer. Msg contains both OUT (ByteBuffer to which the Client writes the request and the Server
 * writes the response) and IN buffer (ByteBuffer to which the Client receives the Response
 * from Server and the Server receives the request from Client).
 * 
 */
public class Msg {

	// private static final Log LOG = LogFactory.getLog(Msg.class.getCanonicalName());
	private long       refToCObject;
	private Eventable  clientSession;
	// reference to MsgPool holding this buffer
	private MsgPool    msgPool;
	private ByteBuffer in, out;
	private Object     userContext;  // variable for usage by the user

	Msg(ByteBuffer buffer, int inSize, int outSize, long id, MsgPool msgPool) {
		this.msgPool = msgPool;
		this.refToCObject = id;
		this.in = createSubBuffer(0, inSize, buffer);
		this.out = createSubBuffer(inSize, inSize + outSize, buffer);
		resetPositions();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("jxio.Msg(" + Long.toHexString(refToCObject) + ")");
		sb.append("[msgIn=" + toStringBB(this.in));
		sb.append(", msgOut=" + toStringBB(this.out));
		sb.append(", msgPool=" + this.msgPool + "]");
		return sb.toString();
	}

	/**
	 * Returns a Msg to the MsgPool to which it belongs. This method should be called only on
	 * Client side - when the application finished handling the message: only when the
	 * response from the server arrives or if Client.sendRequest failed.
	 * 
	 */
	public void returnToParentPool() {
		msgPool.releaseMsg(this);
	}

	/**
	 * Returns ByteBuffer to which the other side has written
	 * <p>
	 * For server this will be the request from client and for client this will be the response from server
	 * 
	 * @return ByteBuffer to which the other side has written
	 */
	public ByteBuffer getIn() {
		return in;
	}

	/**
	 * Returns ByteBuffer that will be sent to the other side
	 * <p>
	 * For server this will be the response to client and for client this will be the request for server
	 * 
	 * @return ByteBuffer that will be sent to the other side
	 */
	public ByteBuffer getOut() {
		return out;
	}

	/**
	 * Retrieves user context associated with this Msg
	 * 
	 * @return user context associated with this Msg
	 */
	public Object getUserContext() {
		return userContext;
	}

	/**
	 * Sets user context to be associated with this Msg. It can be retrieved later
	 * 
	 * @param userContext
	 *            to be kept in this Msg. It can be retrieved later
	 */
	public void setUserContext(Object userContext) {
		this.userContext = userContext;
	}

	/**
	 * This method sets IN & OUT ByteBuffer position to 0 and sets limit to 0
	 * 
	 */
	public void resetPositions() {
		this.in.position(0).limit(0);
		this.out.clear();
	}

	MsgPool getParentPool() {
		return msgPool;
	}

	void setClientSession(Eventable clientSession) {
		this.clientSession = clientSession;
	}

	Eventable getClientSession() {
		return clientSession;
	}

	long getId() {
		return refToCObject;
	}

	private ByteBuffer createSubBuffer(int position, int limit, ByteBuffer buf) {
		ByteBuffer sub;
		buf.position(position);
		buf.limit(limit);
		sub = buf.slice();
		return sub;
	}

	private String toStringBB(ByteBuffer bb) {
		StringBuffer sb = new StringBuffer();
		sb.append("[pos=");
		sb.append(bb.position());
		sb.append(" lim=");
		sb.append(bb.limit());
		sb.append(" cap=");
		sb.append(bb.capacity());
		sb.append("]");
		return sb.toString();
	}
}
