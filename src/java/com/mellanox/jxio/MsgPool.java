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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.impl.Bridge;

public class MsgPool {
	private static final Log LOG     = LogFactory.getLog(MsgPool.class.getCanonicalName());
	private final int        capacity;
	private final int        inSize;
	private final int        outSize;
	private final ByteBuffer buffer;
	private final long       refToCObject;
	List<Msg>                listMsg = new ArrayList<Msg>();

	public MsgPool(int capacity, int inSize, int outSize) {
		this.capacity = capacity;
		this.inSize = inSize;
		this.outSize = outSize;
		long refToCObjects[] = new long[capacity + 1]; // the first element represents the id of MsgPool
		buffer = Bridge.createMsgPool(capacity, inSize, outSize, refToCObjects);
		if (buffer == null) {
			LOG.fatal("there was an error creating the MsgPool");
			refToCObject = 0;
			return;
			// TODO: throw exception
		}
		refToCObject = refToCObjects[0];
		int msgBufferSize = inSize + outSize;

		for (int i = 0; i < capacity; i++) {
			buffer.position(msgBufferSize * i);
			ByteBuffer partialBuffer = buffer.slice();
			partialBuffer.limit(msgBufferSize);
			Msg m = new Msg(partialBuffer, inSize, outSize, refToCObjects[i + 1], this);
			listMsg.add(m);
		}
	}

	public String toString() {
		return getClass().getName() 
				+ "[count=" + count() 
				+ ", capacity=" + capacity 
				+ ", inSize=" + inSize 
				+ ", outSize=" + outSize
				+ "]";		
	}

	// Returns true if this MsgPool contains no elements.
	public boolean isEmpty() {
		return listMsg.size() == 0;		
	}

	// Returns the number of Msgs this MsgPool was created with.
	public int capacity() {
		return this.capacity;
	}

	// Returns the number of Msgs the in this MsgPool.
	public int count() {
		return listMsg.size();
	}

	public Msg getMsg() {
		if (listMsg.isEmpty()) {
			LOG.warn("there are no more messages in pool");
			return null;
		}
		Msg msg = listMsg.remove(0); // 1 is for debugging. should be 0
		return msg;
	}

	public void releaseMsg(Msg msg) {
		if (msg.getParentPool() == this) {
			msg.resetPositions();
			listMsg.add(msg);
		} else {
			LOG.error("parent pool and actual msg pool do not match!");
		}
	}

	public long getId() {
		return refToCObject;
	}

	public void deleteMsgPool(){
		Bridge.deleteMsgPool(refToCObject);
	}

	List<Msg> getAllMsg() {
		return listMsg;
	}
}