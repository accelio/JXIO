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
package org.accelio.jxio.jxioConnection.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.Msg;
import org.accelio.jxio.MsgPool;

public class JxioResourceManager {

	private final static Log                                LOG      = LogFactory.getLog(JxioResourceManager.class
	                                                                         .getCanonicalName());
	private static HashMap<String, LinkedList<MsgPool>>     msgPools = new HashMap<String, LinkedList<MsgPool>>();
	private static ConcurrentLinkedQueue<EventQueueHandler> eqhs     = new ConcurrentLinkedQueue<EventQueueHandler>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				cleanCache();
			}
		});
	}

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

	public static EventQueueHandler getEqh() {
		EventQueueHandler eqh = eqhs.poll();
		if (eqh == null) {
			eqh = new EventQueueHandler(null);
		}
		return eqh;
	}

	public static void returnEqh(EventQueueHandler eqh) {
		eqhs.add(eqh);
	}

	private static void cleanCache() {
		Iterator<Map.Entry<String, LinkedList<MsgPool>>> iterator = msgPools.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, LinkedList<MsgPool>> entry = iterator.next();
			LinkedList<MsgPool> mps = entry.getValue();
			for (MsgPool mp : mps) {
				mp.deleteMsgPool();
			}
		}
		for (EventQueueHandler eqh : eqhs) {
			eqh.close();
		}
		msgPools.clear();
		eqhs.clear();
	}
}
