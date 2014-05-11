package com.mellanox.jxio.jxioConnection.impl;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;

public class JxioResourceManager {

	private final static Log                            LOG      = LogFactory.getLog(JxioResourceManager.class
	                                                                     .getCanonicalName());
	private static HashMap<String, LinkedList<MsgPool>> msgPools = new HashMap<String, LinkedList<MsgPool>>();

	public static MsgPool getMsgPool(int size, int in, int out) {
		MsgPool pool = null;
		String key = getMsgPoolKey(size, in, out);

		synchronized (msgPools) {
			LinkedList<MsgPool> list = msgPools.get(key);

			if (list == null) {
				list = new LinkedList<MsgPool>();
				msgPools.put(key, list);
				pool = new MsgPool(size, in, out);
			} else if (list.isEmpty()) {
				pool = new MsgPool(size, in, out);
			} else {
				pool = list.poll();
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("returning " + pool);
		}
		return pool;

	}

	public static void returnMsgPool(MsgPool pool) {

		if (pool == null) {
			LOG.error("Returning empty pool");
			return;
		}
		Msg msg = pool.getMsg();
		int in = msg.getIn().capacity();
		int out = msg.getOut().capacity();
		pool.releaseMsg(msg);

		String key = getMsgPoolKey(pool.capacity(), in, out);
		synchronized (msgPools) {
			LinkedList<MsgPool> list = msgPools.get(key);
			if (list == null) {
				LOG.error("Returning pool that was not created with ResourceManager");
				return;
			}
			list.add(pool);
			msgPools.put(key, list);
		}
	}

	public static String getMsgPoolKey(int size, int in, int out) {
		return size + "|" + in + "|" + out;
	}
}
