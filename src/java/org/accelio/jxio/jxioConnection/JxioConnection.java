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
package org.accelio.jxio.jxioConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.Msg;
import org.accelio.jxio.jxioConnection.impl.BufferSupplier;
import org.accelio.jxio.jxioConnection.impl.MultiBufOutputStream;
import org.accelio.jxio.jxioConnection.impl.MultiBuffInputStream;
import org.accelio.jxio.jxioConnection.impl.SimpleConnection;

public class JxioConnection {
	private int              isMsgPoolCount = Constants.CLIENT_INPUT_BUF_COUNT;
	private int              osMsgPoolCount = Constants.CLIENT_OUTPUT_BUF_COUNT;
	private static final Log LOG            = LogFactory.getLog(JxioConnection.class.getCanonicalName());
	private InputStream      input          = null;
	private OutputStream     output         = null;
	private final String     name;
	private ISConnection     isCon;
	private OSConnection     osCon;
	private URI              uri;

	/**
	 * Ctor
	 * 
	 * @param uri
	 */
	public JxioConnection(URI uri) throws ConnectException {
		name = "jxioConnection[" + Thread.currentThread().toString() + "]";
		LOG.info("[" + this.toString() + "] " + uri.getHost() + " port " + uri.getPort());
		this.uri = uri;
	}

	private URI appendStreamType(String type) throws ConnectException {
		String uriStr = uri.toString();
		if (uri.getQuery() == null) {
			uriStr += "?stream=" + type;
		} else {
			uriStr += "&stream=" + type;
		}
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			throw new ConnectException("could not append the stream type");
		}
		return uri;
	}

	public InputStream getInputStream() throws ConnectException {
		if (input == null) {
			isCon = new ISConnection(appendStreamType("input"), Constants.MSGPOOL_BUF_SIZE, 0, isMsgPoolCount);
			input = new MultiBuffInputStream(isCon);
		}
		return input;
	}

	public OutputStream getOutputStream() throws ConnectException {
		if (output == null) {
			osCon = new OSConnection(appendStreamType("output"), 0, Constants.MSGPOOL_BUF_SIZE, osMsgPoolCount);
			output = new MultiBufOutputStream(osCon);
		}
		return output;
	}

	public void disconnect() {
		LOG.info(this.toString() + " disconnecting");
		if (isCon != null) {
			isCon.disconnect();
		}
		if (osCon != null) {
			osCon.disconnect();
		}
	}

	private class ISConnection extends SimpleConnection implements BufferSupplier {
		private final String name;

		public ISConnection(URI uri, int msgIn, int msgOut, int msgCount) throws ConnectException {
			super(uri, msgIn, msgOut, msgCount);
			name = "ISConnection[" + cs.toString() + "]";
			try {
				for (int i = 0; i < msgPool.capacity(); i++) {
					Msg msg = msgPool.getMsg();
					cs.sendRequest(msg);
				}
			} catch (IOException e) {
				throw new ConnectException(this.toString() + " Error connecting to server: " + e.getMessage());
			}
			LOG.debug(this.toString() + " created");
		}

		public ByteBuffer getNextBuffer() throws IOException {
			if (msg != null) {
				msg.resetPositions();
			}
			sendMsg();
			do {
				eqh.runEventLoop(1, EventQueueHandler.INFINITE_DURATION);
				if (close) {
					releaseResources();
					throw new IOException("Session was closed, no buffer avaliable");
				}
			} while (msg == null);

			return msg.getIn();
		}

		public void closeStream() {
			try {
				input.close();
			} catch (IOException e) {
				LOG.error(this.toString() + " Could not close inputStream");
			}
		}

		public void handleLastMsg() {
			if (msg != null) {
				msgPool.releaseMsg(msg); // release last msg recieved
				msg = null;
			}
		}

		@Override
		public void flush() {
			throw new UnsupportedOperationException("flush is not supported in inputstream");
		}

		public String toString() {
			return this.name;
		}
	}

	private class OSConnection extends SimpleConnection implements BufferSupplier {
		private final String name;

		public OSConnection(URI uri, int msgIn, int msgOut, int msgCount) throws ConnectException {
			super(uri, msgIn, msgOut, msgCount);
			name = "OSConnection[" + cs.toString() + "]";
		}

		public ByteBuffer getNextBuffer() throws IOException {
			sendMsg();
			if (!msgPool.isEmpty()) {
				msg = msgPool.getMsg();
			} else {
				do {
					eqh.runEventLoop(1, EventQueueHandler.INFINITE_DURATION);
					if (close) {
						releaseResources();
						throw new IOException("Session was closed, no buffer avaliable");
					}
				} while (msg == null);
			}
			msg.resetPositions();
			return msg.getOut();
		}

		public void closeStream() {
			try {
				output.close();
			} catch (IOException e) {
				LOG.error(this.toString() + " Could not close outputStream");
			}
		}

		public void handleLastMsg() {
			flush();
		}

		@Override
		public void flush() {
			sendMsg();
		}

		public String toString() {
			return this.name;
		}
	}

	public void setRcvSize(long mem) throws UnsupportedOperationException {
		if (input != null) {
			throw new UnsupportedOperationException("Memory can be set only before creating InputStream");
		}
		isMsgPoolCount = (int) Math.ceil((double) mem / Constants.MSGPOOL_BUF_SIZE);
	}

	public void setSendSize(long mem) throws UnsupportedOperationException {
		if (output != null) {
			throw new UnsupportedOperationException("Memory can be set only before creating OutputStream");
		}
		osMsgPoolCount = (int) Math.ceil((double) mem / Constants.MSGPOOL_BUF_SIZE);
	}

	public int getRcvSize() {
		return isMsgPoolCount;
	}

	public int getSendSize() {
		return osMsgPoolCount;
	}

	public String toString() {
		return this.name;
	}
}
