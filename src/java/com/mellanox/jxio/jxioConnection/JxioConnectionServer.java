package com.mellanox.jxio.jxioConnection;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.EventReason;
import com.mellanox.jxio.ServerPortal;
import com.mellanox.jxio.ServerSession;
import com.mellanox.jxio.ServerSession.SessionKey;
import com.mellanox.jxio.jxioConnection.JxioConnection;
import com.mellanox.jxio.jxioConnection.impl.*;

public class JxioConnectionServer extends Thread {
	public static final int                            msgPoolBuffSize = 64 * 1024;
	public static final int                            msgPoolnumMsgs  = 164;
	private static final Log                           LOG             = LogFactory.getLog(JxioConnectionServer.class
	                                                                           .getCanonicalName());
	private final String                               name;
	private final EventQueueHandler                    listen_eqh;
	private final ServerPortal                         listener;
	private final JxioConnectionServer.Callbacks       appCallbacks;
	private int                                        numOfWorkers;
	private boolean                                    close           = false;
	private static ConcurrentLinkedQueue<ServerWorker> SPWorkers       = new ConcurrentLinkedQueue<ServerWorker>();

	// private static ConcurrentLinkedQueue<SessionKey> waitingSession;
	// private static Object lock = new Object();

	/**
	 * Ctor that receives from user number of messages to use in the jxio msgpool
	 * 
	 * @param uri
	 * @param numWorkers
	 *            - number of worker threads to start with (this number will grow on demand
	 * @param appCallbacks
	 *            - application callbacks - what to do on new session event
	 * @param msgPoolCount
	 *            - actual amount of messages that will be used is msgPoolCount - numMsgsToAdd
	 */
	public JxioConnectionServer(URI uri, int numWorkers, JxioConnectionServer.Callbacks appCallbacks) {
		this.appCallbacks = appCallbacks;
		numOfWorkers = numWorkers;
		listen_eqh = new EventQueueHandler(null);
		listener = new ServerPortal(listen_eqh, uri, new PortalServerCallbacks());
		name = "[JxioConnectionServer " + listener.toString() + " ]";
		for (int i = 1; i <= numWorkers; i++) {
			SPWorkers.add(new ServerWorker(i, listener.getUriForServer(), appCallbacks));
		}
		LOG.info(this.toString() + " JxioConnectionServer started, host " + uri.getHost() + " listening on port "
		        + uri.getPort() + ", numWorkers " + numWorkers);
		// waitingSession = new ConcurrentLinkedQueue<SessionKey>();
	}

	/**
	 * Thread entry point when running as a new thread
	 */
	public void run() {
		work();
	}

	/**
	 * Entry point when running on the user thread
	 */
	public void work() {
		for (ServerWorker worker : SPWorkers) {
			worker.start();
		}
		listen_eqh.run();
	}

	private static ServerWorker getNextWorker() {
		ServerWorker s = SPWorkers.poll();
		return s;
	}

	public static void updateWorkers(ServerWorker s) {
		// synchronized (lock) {
		// SessionKey ses = waitingSession.poll();
		// if (ses != null) {
		// forwardnewSession(ses, s);
		// } else {
		SPWorkers.add(s);
		// }
		// }

	}

	private void forwardnewSession(SessionKey ses, ServerWorker s) {
		ServerSession session = new ServerSession(ses, s.getSessionCallbacks());
		URI uri;
		try {
			uri = new URI(ses.getUri());
			s.prepareSession(session, uri);
			listener.forward(s.getPortal(), session);
		} catch (URISyntaxException e) {
			LOG.fatal("URI could not be parsed");
		}
	}

	/**
	 * Callbacks for the listener server portal
	 */
	public class PortalServerCallbacks implements ServerPortal.Callbacks {

		public void onSessionEvent(EventName session_event, EventReason reason) {
			LOG.info(JxioConnectionServer.this.toString() + " GOT EVENT " + session_event.toString() + "because of "
			        + reason.toString());
		}

		public void onSessionNew(SessionKey sesKey, String srcIP) {
			if (close || sesKey.getUri().contains("reject=1")) {
				LOG.info(JxioConnectionServer.this.toString() + "Rejecting session");
				listener.reject(sesKey, EventReason.NOT_SUPPORTED, "");
				return;
			}
			// forward the created session to the ServerPortal
			// synchronized (lock) {
			ServerWorker spw = getNextWorker();
			if (spw == null) {
				LOG.info(this.toString() + " No free workers, adding new worker");
				// waitingSession.add(sesKey);
				numOfWorkers++;
				spw = new ServerWorker(numOfWorkers, listener.getUriForServer(), appCallbacks);
				spw.start();
			}
			LOG.info(JxioConnectionServer.this.toString() + " Server worker number " + spw.portalIndex
			        + " got new session");
			forwardnewSession(sesKey, spw);
			// }
		}
	}

	public String toString() {
		return this.name;
	}

	/**
	 * 
	 * The object that implements this interface needs to be a thread
	 */
	public static interface Callbacks {
		public void newSessionOS(URI uri, OutputStream out);

		public void newSessionIS(URI uri, InputStream in);
	}

	/**
	 * Disconnect the server and all worker threads, This can't be undone
	 */
	public void disconnect() {
		listen_eqh.stop();
		close = true;
		for (Iterator<ServerWorker> it = SPWorkers.iterator(); it.hasNext();) {
			it.next().close();
		}
	}
}
