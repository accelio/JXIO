package com.mellanox.jxio.jxioConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.nio.ByteBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.jxioConnection.impl.BufferSupplier;
import com.mellanox.jxio.jxioConnection.impl.MultiBufOutputStream;
import com.mellanox.jxio.jxioConnection.impl.MultiBuffInputStream;
import com.mellanox.jxio.jxioConnection.impl.SimpleConnection;

public class JxioConnection {
	private int              isMsgPoolCount = 100;
	private int              osMsgPoolCount = 100;
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

	public InputStream getInputStream() throws ConnectException {
		if (input == null) {
			isCon = new ISConnection(uri, JxioConnectionServer.msgPoolBuffSize, 0, isMsgPoolCount);
			input = new MultiBuffInputStream(isCon);
		}
		return input;
	}

	public OutputStream getOutputStream() throws ConnectException {
		if (output == null) {
			osCon = new OSConnection(uri, 0, JxioConnectionServer.msgPoolBuffSize, osMsgPoolCount);
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
			for (int i = 0; i < msgPool.capacity(); i++) {
				Msg msg = msgPool.getMsg();
				cs.sendRequest(msg);
			}
			LOG.info(this.toString() + " created");
		}

		public ByteBuffer getNextBuffer() throws IOException {
			if (msg != null) {
				msg.resetPositions();
			}
			sendMsg();
			do {
				eqh.runEventLoop(1, -1);
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
			LOG.info(this.toString() + " created");
		}

		public ByteBuffer getNextBuffer() throws IOException {
			sendMsg();
			if (!msgPool.isEmpty()) {
				msg = msgPool.getMsg();
			} else {
				do {
					eqh.runEventLoop(1, -1);
					if (close) {
						releaseResources();
						throw new IOException("Session was closed, no buffer avaliable");
					}
				} while (msg == null && !close);
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
		isMsgPoolCount = (int) Math.ceil((double) mem / JxioConnectionServer.msgPoolBuffSize);
	}

	public void setSendSize(long mem) throws UnsupportedOperationException {
		if (output != null) {
			throw new UnsupportedOperationException("Memory can be set only before creating OutputStream");
		}
		osMsgPoolCount = (int) Math.ceil((double) mem / JxioConnectionServer.msgPoolBuffSize);
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
