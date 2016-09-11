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
package org.accelio.jxio;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.Msg;
import org.accelio.jxio.impl.Bridge;

/**
 * Both client and server use MsgPool (different instance of the same object).
 * Server side must bind the pool to EventQueueHandler.
 * Each Msg in MsgPool will be zero copied via the RDMA transport to the remote peer.
 * <p>
 * On ServerSize the capacity should be bigger by 64 than on Client Size. InSize and outSize for client and server need
 * to be reversed. For example:
 * <p>
 * Client: MsgPool p1 = new MsgPool(100, 8192, 64)
 * <p>
 * Server: MsgPool p2 = new MsgPool(164, 64, 8192)
 */
public class MsgPool {
	private static final Log   LOG          = LogFactory.getLog(MsgPool.class.getCanonicalName());
	public static final int    MAX_CAPACITY = 100064;
	private final int          capacity;
	private final int          inSize;
	private final int          outSize;
	private final ByteBuffer   buffer;
	private final long         refToCObject;
	ConcurrentLinkedQueue<Msg> listMsg      = new ConcurrentLinkedQueue<Msg>();
	// this flag indicated if this pool is already bound to eqh
	private boolean          already_bound;

	/**
	 * Constructor of MsgPool. Creates MsgPool (including allocating and RDMA registering the memory in C).
	 * 
	 * @param capacity
	 *            - number of msg that this pool will contain. Max is {@value MAX_CAPACITY}
	 * @param inSize
	 *            - size (in bytes) of the receive buffer. For client this will be the response from the server
	 *            and for the server this will be the request from the client
	 * @param outSize
	 *            - size (in bytes) of the send buffer. For client this will be the request to the server
	 *            and for the server this will be the response to the client.
	 * 
	 */
	public MsgPool(int capacity, int inSize, int outSize) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("MP CTOR entry");
		}
		if (capacity > MsgPool.MAX_CAPACITY) {
			LOG.warn("Can't create pool with capacity bigger than maximum. Creating pool with capacity "
			        + MsgPool.MAX_CAPACITY);
			this.capacity = MsgPool.MAX_CAPACITY;
		} else {
			this.capacity = capacity;
		}
		this.inSize = inSize;
		this.outSize = outSize;
		long refToCObjects[] = new long[this.capacity + 1]; // the first element represents the id of MsgPool
		buffer = Bridge.createMsgPool(this.capacity, inSize, outSize, refToCObjects);
		if (buffer == null) {
			LOG.fatal("there was an error creating the MsgPool. Capacity=" + this.capacity + 
					", inSize=" + inSize + ", outSize=" + outSize);
			refToCObject = 0;
			return;
			// TODO: throw exception
		}
		refToCObject = refToCObjects[0];
		int msgBufferSize = inSize + outSize;

		for (int i = 0; i < this.capacity; i++) {
			buffer.position(msgBufferSize * i);
			ByteBuffer partialBuffer = buffer.slice();
			partialBuffer.limit(msgBufferSize);
			Msg m = new Msg(partialBuffer, inSize, outSize, refToCObjects[i + 1], this);
			listMsg.add(m);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "MP CTOR done");
		}
	}

	/**
	 * Returns true if this MsgPool is empty
	 * 
	 * @return true if this MsgPool is empty
	 */
	public boolean isEmpty() {
		return listMsg.isEmpty();
	}

	/**
	 * Returns the number of Msgs this MsgPool was created with.
         *
         * @return capacity of this MsgPool
	 */
	public int capacity() {
		return this.capacity;
	}

	/**
	 * Returns the number of Msgs the in this MsgPool.
	 * 
	 * @return number of Msgs in MsgPool
	 */
	public int count() {
		return listMsg.size();
	}

	/**
	 * Returns a Msg from the pool (or null if pool is empty).
	 * This method should be called on client side.
	 * 
	 * @return Msg from the pool (or null if pool is empty).
	 */
	public Msg getMsg() {
		if (listMsg.isEmpty()) {
			LOG.warn(this.toLogString() + "there are no more messages in pool");
			return null;
		}
		Msg msg = listMsg.poll();
		msg.setMsgReturnable();
		return msg;
	}

	/**
	 * Returns msg back to pool.
	 * This method should be called on client side, once the application
	 * finished handling the msg. This method should only be called for Msg that
	 * was obtained by MsgPool.getMsg() method
	 * 
	 * @param msg
	 *            to be returned back to pool
	 * @return true if Msg was returned to pool and false if this msg can not be returned to pool
	 */
	public boolean releaseMsg(Msg msg) {
		if (msg.getParentPool() == this) {
			if (msg.isReturnable()) {
				msg.resetPositions();
				msg.setMsgNotReturnable();
				listMsg.add(msg);
				return true;
			} else {
				LOG.error(this.toLogString() + "Msg " + msg.toString()
				        + " can not be returned to pool, since it was not obtained using pool.getMsg method");
				return false;
			}
		}
		LOG.error(this.toLogString() + "parent pool " + msg.getParentPool().getId() + " and actual msg pool "
		        + this.getId() + " do not match!");
		return false;
	}

	/**
	 * Returns id of the object. The id is unique and represents pointer
	 * to the corresponding C object.
	 * 
	 * @return id of the object
	 */
	public long getId() {
		return refToCObject;
	}

	/**
	 * Deletes this MsgPool. This method releases all memory allocated in C
	 * and therefore this should be the last method called for this MsgPool
	 */
	public void deleteMsgPool() {
		Bridge.deleteMsgPool(refToCObject);
	}

	ConcurrentLinkedQueue<Msg> getAllMsg() {
		return listMsg;
	}

	boolean isBounded() {
		return already_bound;
	}

	void setIsBounded(boolean already_bound) {
		this.already_bound = already_bound;
	}

	int getInSize() {
		return this.inSize;
	}

	int getOutSize() {
		return this.outSize;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("jxio.MsgPool(" + Long.toHexString(refToCObject) + ")");
		sb.append("[count=" + count());
		sb.append(", capacity=" + capacity);
		sb.append(", inSize=" + inSize);
		sb.append(", outSize=" + outSize + "]");
		return sb.toString();
	}

	private String toLogString() {
		return this.toString() + ": ";
	}
}
