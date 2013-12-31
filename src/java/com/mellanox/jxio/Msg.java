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

public class Msg {

	//private static final Log LOG = LogFactory.getLog(Msg.class.getCanonicalName());
	private long             refToCObject;
	private Eventable        clientSession;
	private MsgPool          msgPool;                                              // reference to MsgPool holding this
																					// buffer
	private ByteBuffer       in, out;
	private Object           userContext;                                          // variable for usage by the user
	Msg(ByteBuffer buffer, int inSize, int outSize, long id, MsgPool msgPool) {
		this.msgPool = msgPool;
		this.refToCObject = id;
		this.in = createSubBuffer(0, inSize, buffer);
		this.out = createSubBuffer(inSize, inSize+outSize, buffer);
		resetPositions();
		//if (LOG.isTraceEnabled()) {
		//	LOG.trace(this);
		//}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("jxio.Msg(" + Long.toHexString(refToCObject) + ")");
		sb.append("[msgIn=" + toStringBB(this.in));
		sb.append(", msgOut=" + toStringBB(this.out));
		sb.append(", msgPool=" + this.msgPool + "]");
		return sb.toString();
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

	public Object getUserContext() {
		return userContext;
	}

	public void setUserContext(Object userContext) {
		this.userContext = userContext;
	}

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
