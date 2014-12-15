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

import java.net.ConnectException;
import java.net.URI;

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
import org.accelio.jxio.jxioConnection.impl.JxioResourceManager;
import org.accelio.jxio.jxioConnection.impl.SimpleConnection;

public abstract class SimpleConnection {

	private static final Log      LOG              = LogFactory.getLog(SimpleConnection.class.getCanonicalName());
	private boolean               established      = false;
	protected EventQueueHandler   eqh;
	protected final ClientSession cs;
	protected MsgPool             msgPool;
	protected EventName           connectErrorType = null;
	protected boolean             close            = false;
	protected Msg                 msg              = null;

	public SimpleConnection(URI uri, int msgIn, int msgOut, int msgCount) throws ConnectException {
		eqh = JxioResourceManager.getEqh();
		msgPool = JxioResourceManager.getMsgPool(msgCount, msgIn, msgOut);
		long startTime = System.nanoTime();
		cs = new ClientSession(eqh, uri, new ClientCallbacks());
		eqh.runEventLoop(1, EventQueueHandler.INFINITE_DURATION); // session established event
		if (!established) {
			throw new ConnectException(this.toString() + " could not connect to " + uri.getHost() + " on port "
			        + uri.getPort() + ", got " + connectErrorType);
		}
		long endTime = System.nanoTime();

		LOG.info(cs.toString() + " session established with host " + uri.getHost() + ", time taken to open: "
		        + (endTime - startTime));
	}

	public class ClientCallbacks implements ClientSession.Callbacks {

		public void onMsgError(Msg msg, EventReason reason) {
			if (reason != EventReason.MSG_FLUSHED) {
				LOG.info(SimpleConnection.this.toString() + " onMsgErrorCallback, " + reason);
			}
			msgPool.releaseMsg(msg);
		}

		public void onSessionEstablished() {
			established = true;
		}

		public void onSessionEvent(EventName event, EventReason reason) {
			LOG.info(SimpleConnection.this.toString() + " onSessionEvent " + event);
			if (event == EventName.SESSION_CLOSED || event == EventName.SESSION_ERROR
			        || event == EventName.SESSION_REJECT) { // normal exit
				connectErrorType = event;
				boolean needToClean = !eqh.getInRunEventLoop();
				if (close) {
					needToClean = false;
				}
				close = true;
				eqh.breakEventLoop();
				if (needToClean) {
					releaseResources();
				}
			}
		}

		public void onResponse(Msg m) {
			msg = m;
			if (close) {
				msgPool.releaseMsg(m);
				msg = null;
			}
		}
	}

	public void disconnect() {
		if (close) return;
		close = true;
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toString() + " jxioConnection disconnect, msgPool.count()" + msgPool.count()
			        + " msgPool.capacity() " + msgPool.capacity());
		}
		handleLastMsg();
		while (msgPool.count() < msgPool.capacity()) {
			eqh.runEventLoop(msgPool.capacity() - msgPool.count(), EventQueueHandler.INFINITE_DURATION);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toString() + "disconnecting,got all msgs back, closing session");
		}
		closeStream();
		cs.close();
		eqh.runEventLoop(EventQueueHandler.INFINITE_EVENTS, EventQueueHandler.INFINITE_DURATION);
		releaseResources();
	}

	public void releaseResources() {
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toString() + " Releasing resources");
		}
		if (eqh != null)
			JxioResourceManager.returnEqh(eqh);
		if (msgPool != null)
			JxioResourceManager.returnMsgPool(msgPool);
		eqh = null;
		msgPool = null;
	}

	public void sendMsg() {
		if (msg != null) {
			try {
				cs.sendRequest(msg);
			} catch (JxioSessionClosedException e) {
				LOG.debug(this.toString() + " Error sending message: " + e.toString());
				msgPool.releaseMsg(msg);
			} catch (JxioQueueOverflowException e) {
				LOG.error(this.toString() + " Error sending message: " + e.toString());
				msgPool.releaseMsg(msg);
			} catch (JxioGeneralException e) {
				LOG.error(this.toString() + " Error sending message: " + e.toString());
				msgPool.releaseMsg(msg);
			}
			msg = null;
		}
	}

	public abstract void closeStream();

	public abstract void handleLastMsg();
}
