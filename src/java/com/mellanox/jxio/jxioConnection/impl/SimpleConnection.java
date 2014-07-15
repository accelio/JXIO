package com.mellanox.jxio.jxioConnection.impl;

import java.net.ConnectException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.ClientSession;
import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.EventReason;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;

public abstract class SimpleConnection {

	private static final Log      LOG         = LogFactory.getLog(SimpleConnection.class.getCanonicalName());
	private boolean               established = false;
	protected EventQueueHandler   eqh;
	protected final ClientSession cs;
	protected MsgPool             msgPool;
	protected EventName           connectErrorType;
	protected boolean             close       = false;
	protected Msg                 msg         = null;

	public SimpleConnection(URI uri, int msgIn, int msgOut, int msgCount) throws ConnectException {
		eqh = JxioResourceManager.getEqh();
		msgPool = JxioResourceManager.getMsgPool(msgCount, msgIn, msgOut);
		long startTime = System.nanoTime();
		cs = new ClientSession(eqh, uri, new ClientCallbacks());
		eqh.runEventLoop(1, -1); // session established event
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

		public void onSessionEvent(EventName session_event, EventReason reason) {
			LOG.info(SimpleConnection.this.toString() + " onSessionEvent " + session_event);
			if (session_event == EventName.SESSION_CLOSED || session_event == EventName.SESSION_ERROR
			        || session_event == EventName.SESSION_REJECT) { // normal exit
				connectErrorType = session_event;
				close = true;
				closeStream();
				eqh.breakEventLoop();
				releaseResources();
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
		close = true;
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toString() + " jxioConnection disconnect, msgPool.count()" + msgPool.count()
			        + " msgPool.capacity() " + msgPool.capacity());
		}
		handleLastMsg();
		while (msgPool.count() < msgPool.capacity()) {
			eqh.runEventLoop(msgPool.capacity() - msgPool.count(), -1);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toString() + "disconnecting,got all msgs back, closing session");
		}
		closeStream();
		cs.close();
		eqh.runEventLoop(-1, -1);
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
			cs.sendRequest(msg);
			msg = null;
		}
	}

	public abstract void closeStream();
	public abstract void handleLastMsg();
}
