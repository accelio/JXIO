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
package org.accelio.jxio.tests.benchmarks;

import java.net.URI;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.Msg;
import org.accelio.jxio.MsgPool;
import org.accelio.jxio.EventName;
import org.accelio.jxio.EventReason;
import org.accelio.jxio.ClientSession;
import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.exceptions.JxioGeneralException;
import org.accelio.jxio.exceptions.JxioQueueOverflowException;
import org.accelio.jxio.exceptions.JxioSessionClosedException;

public class ClientWorker implements Callable<double[]> {
	private final ClientSession cs;
	private final EventQueueHandler eqh;
	private final MsgPool pool;
	private final int in_msgSize;
	private final int out_msgSize;
	// calculation members
	private boolean firstTime;
	private long startTime;
	private long cnt;
	private int sample_cnt;
	private int res_array_index = 0;
	private int burst_size;

	// results array (in the form of [tps1,bw1,tp2,bw2...]
	public double[] results;

	// logger
	private static Log LOG = LogFactory.getLog(ClientWorker.class.getCanonicalName());

	// cTor
	public ClientWorker(int inMsg_size, int outMsg_size, URI uri, int burst_size, double[] res) {
		eqh = new EventQueueHandler(new ClientEQHCallbacks());
		pool = new MsgPool(burst_size, inMsg_size, outMsg_size);
		results = res;
		cs = new ClientSession(eqh, uri, new ClientWorkerCallbacks());
		in_msgSize = inMsg_size;
		out_msgSize = outMsg_size;
		this.burst_size = burst_size;
		int msgKSize = ((inMsg_size > outMsg_size) ? inMsg_size : outMsg_size) / 1024;
		if (msgKSize == 0) {
			sample_cnt = 40000;
		} else {
			sample_cnt = 40000 / msgKSize;
		}
		cnt = 0;
		firstTime = true;
	}

	public void close() {
		LOG.debug("closing client session");
		cs.close();
	}

	public double[] call() {
		for (int i = 0; i < burst_size; i++) {
			Msg msg = pool.getMsg();
			if (msg == null) {
				LOG.error("Cannot get new message");
				break;
			}
			msg.getOut().position(msg.getOut().capacity()); // simulate 'out_msgSize' was written into buffer
			try {
				cs.sendRequest(msg);
			} catch (JxioGeneralException e) {
				LOG.error("Error sending: " + e.toString());
				pool.releaseMsg(msg);
			} catch (JxioSessionClosedException e) {
				LOG.error("Error sending: session closed " + e.toString());
				pool.releaseMsg(msg);
			} catch (JxioQueueOverflowException e) {
				LOG.error("Error sending: queue overflow " + e.toString());
				pool.releaseMsg(msg);
			}
		}
		eqh.run();
		eqh.close();
		LOG.debug("deleting message pool");
		pool.deleteMsgPool();
		return results;
	}

	// callbacks for the Client's event queue handler
	public class ClientEQHCallbacks implements EventQueueHandler.Callbacks {
		// this method should return an unbinded MsgPool.
		public MsgPool getAdditionalMsgPool(int inSize, int outSize) {
			System.out.println("Messages in Client's message pool ran out, Aborting test");
			return null;
		}
	}

	class ClientWorkerCallbacks implements ClientSession.Callbacks {

		public void onMsgError(Msg msg, EventReason reason) {
			if (ClientWorker.this.cs.getIsClosing()) {
				LOG.debug("On Message Error while closing. Reason is=" + reason);
			} else {
				LOG.error("On Message Error. Reason is=" + reason);
			}
			msg.returnToParentPool();
		}

		public void onSessionEstablished() {
			LOG.debug("Session established");
		}

		public void onSessionEvent(EventName event, EventReason reason) {
			if (event == EventName.SESSION_CLOSED) {
				LOG.debug("closing eqh");
				eqh.stop();
			}
		}

		public void onResponse(Msg msg) {
			if (firstTime) {
				startTime = System.nanoTime();
				firstTime = false;
			}

			cnt++;
			if (cnt == sample_cnt) {
				long delta = System.nanoTime() - startTime;
				// multiply by 10^9 because result is in seconds
				long pps = (cnt * 1000000000) / delta;
				// divide by (1024*1024) in order to get BW in MB
				double out_bw = (1.0 * pps * out_msgSize / (1024 * 1024));
				double in_bw = (1.0 * pps * in_msgSize / (1024 * 1024));
				results[res_array_index] = pps;
				results[res_array_index + 1] = out_bw;
				results[res_array_index + 2] = in_bw;
				res_array_index += 3;
				if (res_array_index == results.length) {
					ClientWorker.this.close();
					return;
				}
				cnt = 0;
				startTime = System.nanoTime();
			}
			if (!ClientWorker.this.cs.getIsClosing()) {
				try {
					cs.sendRequest(msg);
				} catch (JxioGeneralException e) {
					LOG.error("Error sending: " + e.toString());
					pool.releaseMsg(msg);
				} catch (JxioSessionClosedException e) {
					LOG.error("Error sending: session closed " + e.toString());
					pool.releaseMsg(msg);
				} catch (JxioQueueOverflowException e) {
					LOG.error("Error sending: queue overflow " + e.toString());
					pool.releaseMsg(msg);
				}
			}
		}
	}
}
