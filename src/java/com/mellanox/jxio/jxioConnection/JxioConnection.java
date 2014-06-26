package com.mellanox.jxio.jxioConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.ClientSession;
import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.EventReason;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;
import com.mellanox.jxio.jxioConnection.impl.BufferSupplier;
import com.mellanox.jxio.jxioConnection.impl.MultiBufOutputStream;
import com.mellanox.jxio.jxioConnection.impl.MultiBuffInputStream;
import com.mellanox.jxio.jxioConnection.impl.JxioResourceManager;

public class JxioConnection implements BufferSupplier {
	public static final int         msgPoolBuffSize = 64 * 1024;
	private static final Log        LOG             = LogFactory.getLog(JxioConnection.class.getCanonicalName());
	private final EventQueueHandler eqh;
	private final ClientSession     cs;
	private final MsgPool           msgPool;
	private InputStream             input           = null;
	private Msg                     msg             = null;
	private boolean                 established     = false;
	private boolean                 close           = false;
	private int                     count           = 0;
	private final String            name;
	private EventName               connectErrorType;

	/**
	 * Ctor that receives from user the amount of memory to use for the jxio msgpool
	 * 
	 * @param msgPoolMem
	 *            amount of memory
	 * @param uri
	 */
	public JxioConnection(long msgPoolMem, URI uri) throws ConnectException {
		this(uri, (int) Math.ceil((double) msgPoolMem / (double) msgPoolBuffSize));
	}

	/**
	 * Ctor that receives from user number of messages to use in the jxio msgpool
	 * 
	 * @param uri
	 * @param msgPoolCount
	 *            number of messages
	 */
	public JxioConnection(URI uri, int msgPoolCount) throws ConnectException {
		long startTime = System.nanoTime();
		eqh = JxioResourceManager.getEqh();
		cs = new ClientSession(eqh, uri, new ClientCallbacks());
		name = "jxioConnection[" + cs.toString() + "]";
		LOG.info("[" + this.toString() + "] " + uri.getHost() + " port " + uri.getPort());
		msgPool = JxioResourceManager.getMsgPool(msgPoolCount, msgPoolBuffSize, 0);
		eqh.runEventLoop(1, -1); // session established event

		if (!established) {
			throw new ConnectException(this.toString() + " could not connect to " + uri.getHost() + " on port "
			        + uri.getPort() + ", got " + connectErrorType);
		}
		long endTime = System.nanoTime();

		LOG.info(this.toString() + " session established with host " + uri.getHost() + ", time taken to open: "
		        + (endTime - startTime));

		for (int i = 0; i < msgPool.count(); i++) {
			Msg msg = msgPool.getMsg();
			cs.sendRequest(msg);
		}
	}

	public ByteBuffer getNextBuffer() {
		if (msg != null) {
			msg.resetPositions();
			cs.sendRequest(msg);
			msg = null;
		}
		do {
			eqh.runEventLoop(1, -1);
			if (close) {
				releaseResources();
			}
		} while (msg == null);

		return msg.getIn();
	}

	public InputStream getInputStream() {
		if (input == null) {
			input = new MultiBuffInputStream(this);
		}
		return input;
	}

	public void disconnect() {
		close = true;
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toString() + " jxioConnection disconnect, msgPool.count()" + msgPool.count()
			        + " msgPool.capacity() " + msgPool.capacity());
		}
		if (msg != null) {
			msgPool.releaseMsg(msg); // release last msg recieved
		}
		while (msgPool.count() < msgPool.capacity()) {
			eqh.runEventLoop(msgPool.capacity() - msgPool.count(), -1);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toString() + " jxioConnection disconnect,got all msgs back, closing session");
		}
		try {
			input.close();
		} catch (IOException e) {
			LOG.error(this.toString() + " Could not close inputstream");
		}
		cs.close();
		eqh.runEventLoop(-1, -1);

		releaseResources();
	}

	public void releaseResources() {
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toString() + " jxioConnection releaseResources");
		}
		JxioResourceManager.returnEqh(eqh);
		JxioResourceManager.returnMsgPool(msgPool);
	}

	class ClientCallbacks implements ClientSession.Callbacks {

		public void onMsgError(Msg msg, EventReason reason) {
			if (reason != EventReason.MSG_FLUSHED) {
				LOG.info(this.toString() + " onMsgErrorCallback, " + reason);
			}
			msgPool.releaseMsg(msg);
		}

		public void onSessionEstablished() {
			established = true;
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			LOG.info(this.toString() + " onSessionEvent " + session_event);
			if (session_event == EventName.SESSION_CLOSED || session_event == EventName.SESSION_ERROR
			        || session_event == EventName.SESSION_REJECT) { // normal exit
				connectErrorType = session_event;
				eqh.breakEventLoop();
				close = true;
				try {
					input.close();
				} catch (IOException e) {
					LOG.error(this.toString() + " Could not close inputstream");
				}
				releaseResources();
			}
		}

		public void onResponse(Msg msg) {
			count++;
			JxioConnection.this.msg = msg;
			if (close) {
				msgPool.releaseMsg(msg);
			}
		}
	}

	public String toString() {
		return this.name;
	}
}
