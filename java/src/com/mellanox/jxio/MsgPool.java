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
	ByteBuffer               buffer;
	private long             refToCObject;
	List<Msg>                listMsg = new ArrayList<Msg>();

	public MsgPool(int count, int inSize, int outSize) {
		long refToCObjects[] = new long[count + 1]; // the first element represents the id of MsgPool
		buffer = Bridge.createMsgPool(count, inSize, outSize, refToCObjects);
		if (buffer == null) {
			LOG.fatal("there was an error creating the MsgPool");
			return;
			// TODO: throw exception
		}
		refToCObject = refToCObjects[0];
		int msgBufferSize = inSize + outSize;
		if (LOG.isDebugEnabled()) {
			LOG.debug("capacity is " + buffer.capacity() + " limit " + buffer.limit() + " position "
			        + buffer.position() + " remaining is " + buffer.remaining());
		}
		for (int i = 0; i < count; i++) {
			buffer.position(msgBufferSize * i);
			ByteBuffer partialBuffer = buffer.slice();
			partialBuffer.limit(msgBufferSize);
			if (LOG.isDebugEnabled()) {
				LOG.debug("capacity is " + partialBuffer.capacity() + " limit " + partialBuffer.limit() + " position "
				        + partialBuffer.position() + " remaining is " + partialBuffer.remaining());
			}
			Msg m = new Msg(partialBuffer, inSize, outSize, refToCObjects[i + 1], this);
			listMsg.add(m);
			if (LOG.isDebugEnabled()) {
				LOG.debug("ptr is " + refToCObjects[i + 1]);
			}
		}
	}

	public Msg getMsg() {
		if (listMsg.isEmpty()) {
			LOG.error("there are no more messages in pool");
			return null;
		}
		Msg msg = listMsg.remove(0); // 1 is for debugging. should be 0
		return msg;
	}

	public void releaseMsg(Msg msg) {
		if (msg.getParentPool() == this) {
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