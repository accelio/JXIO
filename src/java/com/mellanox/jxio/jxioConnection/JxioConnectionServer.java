package com.mellanox.jxio.jxioConnection;

import java.io.OutputStream;
import java.net.URI;
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
import com.mellanox.jxio.jxioConnection.impl.MultiBufOutputStream;

public class JxioConnectionServer extends Thread {

	private static final Log                           LOG          = LogFactory.getLog(JxioConnectionServer.class
	                                                                        .getCanonicalName());
	private final String                               name;
	private final int                                  numMsgsToAdd = 64;
	private final EventQueueHandler                    listen_eqh;
	private final ServerPortal                         listener;
	private final JxioConnectionServer.Callbacks       appCallbacks;
	private final int                                  numMsg;
	private int                                        numOfWorkers;
	private static ConcurrentLinkedQueue<ServerWorker> SPWorkers    = new ConcurrentLinkedQueue<ServerWorker>();

	// private static ConcurrentLinkedQueue<SessionKey> waitingSession;
	// private static Object lock = new Object();

	/**
	 * Ctor that receives from user the amount of memory to use for the jxio msgpool
	 * 
	 * @param uri
	 * @param msgPoolMem
	 *            - actual amount of memory that will be used for msgs is msgPoolMem -
	 *            numMsgsToAdd*JxioConnection.msgPollBuffSize
	 * @param numWorkers
	 *            - number of worker threads to start with (this number will grow on demand
	 * @param appCallbacks
	 *            - application callbacks - what to do on new session event
	 */
	public JxioConnectionServer(URI uri, long msgPoolMem, int numWorkers, JxioConnectionServer.Callbacks appCallbacks) {
		this(uri, numWorkers, appCallbacks, (int) Math.ceil((double) msgPoolMem / (double) JxioConnection.msgPoolBuffSize));
	}

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
	public JxioConnectionServer(URI uri, int numWorkers, JxioConnectionServer.Callbacks appCallbacks, int msgPoolCount) {
		this.appCallbacks = appCallbacks;
		numOfWorkers = numWorkers;
		this.numMsg = msgPoolCount;
		listen_eqh = new EventQueueHandler(null);
		listener = new ServerPortal(listen_eqh, uri, new PortalServerCallbacks());
		name = "[JxioConnectionServer " + listener.toString() + " ]";
		for (int i = 1; i <= numWorkers; i++) {
			SPWorkers.add(new ServerWorker(i, 0, JxioConnection.msgPoolBuffSize, listener.getUriForServer(), numMsg,
			        appCallbacks));
		}

		LOG.info(this.toString() + " JxioConnectionServer started, host " + uri.getHost() + " listening on port "
		        + uri.getPort());
		// waitingSession = new ConcurrentLinkedQueue<SessionKey>();
	}

	public void run() {
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
		listener.forward(s.getPortal(), session);
		// to get things going - send the established event to client
		s.getEqh().runEventLoop(1, 0);
		s.startSession(session, ses);
	}

	// callbacks for the listener server portal
	public class PortalServerCallbacks implements ServerPortal.Callbacks {

		public void onSessionEvent(EventName session_event, EventReason reason) {
			LOG.info(this.toString() + " GOT EVENT " + session_event.toString() + "because of " + reason.toString());
		}

		public void onSessionNew(SessionKey sesKey, String srcIP) {

			// forward the created session to the ServerPortal
			// synchronized (lock) {
			ServerWorker spw = getNextWorker();
			if (spw == null) {
				LOG.info(this.toString() + " No free workers, adding new worker");
				// waitingSession.add(sesKey);
				numOfWorkers++;
				spw = new ServerWorker(numOfWorkers, 0, JxioConnection.msgPoolBuffSize, listener.getUriForServer(),
				        numMsg, appCallbacks);
				spw.start();
			}
			LOG.info(this.toString() + " Server worker number " + spw.portalIndex + " got new session");
			forwardnewSession(sesKey, spw);
			// }
		}
	}

	public String toString() {
		return this.name;
	}

	/**
	 * The object that implements this interface nedds to be a thread
	 * 
	 * @author dinal
	 * 
	 */
	public static interface Callbacks {
		public void newSessionStart(String uri, OutputStream out);
	}
}
