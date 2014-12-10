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
package org.accelio.jxio;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.EventName;
import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.EventReason;
import org.accelio.jxio.ServerSession;
import org.accelio.jxio.WorkerCache;
import org.accelio.jxio.WorkerCache.Worker;
import org.accelio.jxio.WorkerCache.WorkerProvider;
import org.accelio.jxio.impl.Bridge;
import org.accelio.jxio.impl.Event;
import org.accelio.jxio.impl.EventNewSession;
import org.accelio.jxio.impl.EventSession;
import org.accelio.jxio.impl.EventNameImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 * ServerPortal is the object which listens to incoming connections. He can accept/reject or forward to
 * another portal the new session request. There are two kinds of ServerPortal:
 * 1. listener - listens on a well known port and redirects the session to a different thread (he
 * can also accept the session)
 * 2. worker - sessions are redirected to him. The requests from Client will arrive on this portal
 * ServerPortal receives several events on his lifetime. On each of them a method of interface
 * Callbacks is invoked. User must implement this interface and pass it in ctor.
 * The events are:
 * 1. onSessionNew
 * 2. onSessionEvent
 * 
 * To initialize worker cache for faster connection establish time -
 * 1. each server worker should implement WorkerCache.Worker interface
 * 2. when creating new listener pass in ctor an implementation for WorkerCache.WorkerProvider
 * interface
 *
 */
public class ServerPortal extends EventQueueHandler.Eventable {

	private final Callbacks         callbacks;
	private final EventQueueHandler eqh;
	private final int               port;
	private final String            name;
	private final String            nameForLog;
	private static final Log        LOG      = LogFactory.getLog(ServerPortal.class.getCanonicalName());
	private Set<ServerSession>      sessions = new HashSet<ServerSession>();
	private WorkerCache 			cache = null;
	private URI                     uri;
	private URI                     uriPort0;

	public static interface Callbacks {

		/**
		 * This event is triggered when a request for a new session arrives from Client.
		 * 
		 * @param sesKey
		 *            - SessionKey. Contains long (id of the session) and String (uri of the session)
		 *            Needs to be passed to ServerSession c-tor.
		 * @param srcIP
		 *            - IP of the Client
		 * @param workerHint
		 *            - Hint from cache on what server portal worker the client should connect to
		 *            if cache is not initialized the hint will be null
		 */
		public void onSessionNew(ServerSession.SessionKey sesKey, String srcIP, Worker workerHint);

		/**
		 * This event is triggered when PORTAL_CLOSED event arrives
		 * 
		 * @param event
		 *            - the event that was triggered
		 * @param reason
		 *            - the object containing the reason for triggerring event
		 */
		public void onSessionEvent(EventName event, EventReason reason);
	}

	/**
	 * This constructor is for the ServerPortal that doesn't use worker cache. ServerPortal can be either a listener(listens
	 * on a well known port and redirects the request for a new session to ServerPortal worker) or a worker.
	 * 
	 * @param eqh
	 *            - EventQueueHAndler on which the events
	 *            (onSessionNew, onSessionEvent etc) of this portal will arrive
	 * @param uri
	 *            - on which the ServerPortal will listen. Can contain a well known port
	 * @param callbacks
	 *            - implementation of Interface ServerPortal.Callbacks
	 */
	public ServerPortal(EventQueueHandler eqh, URI uri, Callbacks callbacks) {
		this(eqh, uri, callbacks, null);
	}

	/**
	 * This constructor is for the ServerPortal that uses worker cache. ServerPortal can be either a listener(listens
	 * on a well known port and redirects the request for a new session to ServerPortal worker) or a worker.
	 * 
	 * @param eqh
	 *            - EventQueueHAndler on which the events
	 *            (onSessionNew, onSessionEvent etc) of this portal will arrive
	 * @param uri
	 *            - on which the ServerPortal will listen. Can contain a well known port
	 * @param callbacks
	 *            - implementation of Interface ServerPortal.Callbacks
	 * @param workerProvider
	 *            - implementation of Interface WorkerCache.WorkerProvider
	 */
	public ServerPortal(EventQueueHandler eqh, URI uri, Callbacks callbacks, WorkerCache.WorkerProvider workerProvider) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("SP CTOR entry");
		}
		this.eqh = eqh;
		this.callbacks = callbacks;

		if (!uri.getScheme().equals("rdma") && !uri.getScheme().equals("tcp") ) {
			LOG.fatal("mal formatted URI: " + uri);
		}

		long[] ar = Bridge.startServerPortal(uri.toString(), eqh.getId());
		this.setId(ar[0]);
		this.port = (int) ar[1];
		this.name = "jxio.SP[" + Long.toHexString(getId()) + "]";
		this.nameForLog = this.name + ": ";

		if (getId() == 0) {
			LOG.fatal(this.toLogString() + "there was an error creating ServerPortal");
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "listening to " + uri);
		}
		this.uriPort0 = replacePortInURI(uri, 0);
		this.uri = replacePortInURI(uri, this.port);

		this.eqh.addEventable(this);

		if (workerProvider != null) {
			this.cache = new WorkerCache(workerProvider);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "SP CTOR done");
		}
	}

	/**
	 * Returns URI for ServerPortal listener. This method is called from ServerPortal listener
	 * 
	 * @return URI for ServerPortal listener
	 */
	public URI getUriForServer() {
		return uriPort0;
	}

	/**
	 * This method closes the ServerPortal. The method is asynchronous:
	 * the ServerPortal will be closed only when it receives event POTRTAL_CLOSED.
	 * If there are still ServerSessions on this ServerPortal, they will be closed as well. Only after
	 * SESSION_CLOSED will be recieved on all ServerSessions (if there are any), SERVER_PORTAL event will be
	 * received/.
	 * 
	 * @return true if there was a successful call to close of the ServerPortal object on
	 *         C side and false otherwise
	 */
	public boolean close() {
		if (this.getIsClosing()) {
			LOG.warn(this.toLogString() + "attempting to close server portal that is already closed or being closed");
			return false;
		}
		if (getId() == 0) {
			LOG.error(this.toLogString() + "closing ServerPortal with empty id");
			return false;
		}
		for (ServerSession serverSession : sessions) {
			if (!serverSession.getIsClosing()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(this.toLogString() + "closing serverSession=" + serverSession.getId()
					        + " from ServerPortal.close");
				}
				serverSession.close();
			}
		}

		Bridge.stopServerPortal(getId());
		setIsClosing(true);

		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "close() Done");
		}
		return true;
	}

	/**
	 * This method accepts the serverSession on this ServerPortal. This means that all ServerSession
	 * events will arrive on this ServerPortal's EventQueueHandler.
	 * 
	 * @param serverSession
	 *            - serverSession that will be accepted
	 */
	public void accept(ServerSession serverSession) {
		serverSession.setEventQueueHandlers(this.eqh, this.eqh);
		if (!Bridge.acceptSession(serverSession.getId(), this.getId()))
			LOG.error("accept failed");
		this.setSession(serverSession);
	}

	/**
	 * This method forwards the serverSession on the ServerPortal. This means that all ServerSession
	 * events will arrive on this portal's EventQueueHandler.
	 * 
	 * @param portal
	 *            - the portal to which the serverSession will be forwarded
	 * @param serverSession
	 *            - sesrverSession that will be forwarded
	 */
	public void forward(ServerPortal portal, ServerSession serverSession) {
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "portal " + portal + " ses id is " + serverSession.getId());
		}
		if (portal == this) {// in case forward was called but the user really means accept
			accept(serverSession);
			return;
		}
		URI uriForForward = portal.getUri();
		if (uriForForward.getHost().equals("0.0.0.0")) {
			uriForForward = this.replaceIPinURI(uriForForward, serverSession.getUri());
		}

		serverSession.setEventQueueHandlers(this.eqh, portal.eqh);
		if (!Bridge.forwardSession(uriForForward.toString(), serverSession.getId(), portal.getId()))
			LOG.error("forward failed");
		portal.setSession(serverSession);
	}

	/**
	 * This method rejects the Session.
	 * 
	 * @param sesKey
	 *            which was received in callback onNewSession
	 * @param reason
	 *            - reason to reject the Session
	 * @param data
	 *            - data to pass to the client
	 */
	public void reject(ServerSession.SessionKey sesKey, EventReason reason, String data) {
		Bridge.rejectSession(sesKey.getSessionPtr(), reason.getIndex(), data, data.length());
	}

	/**
	 * Get URI server is listening on
	 * Needed when port is set to 0 on bring up and server chooses a random port
	 * @return actual uri server is listening on
	 */
	public URI getUri() {
		return uri;
	}

	private void setSession(ServerSession serverSession) {
		this.sessions.add(serverSession);
		serverSession.setPortal(this);
	}

	boolean onEvent(Event ev) {
		switch (ev.getEventType()) {

			case 0: // session error event
				if (ev instanceof EventSession) {
					int errorType = ((EventSession) ev).getErrorType();
					int reason = ((EventSession) ev).getReason();
					if (LOG.isDebugEnabled()) {
						LOG.debug(this.toLogString() + "Received Session Event: Type=" + errorType + ", Reason=" + reason);
					}
					EventNameImpl eventName = EventNameImpl.getEventByIndex(errorType);

					if (eventName == EventNameImpl.PORTAL_CLOSED) {
						this.eqh.removeEventable(this);
						if (LOG.isDebugEnabled()) {
							LOG.debug(this.toLogString() + "portal was closed");
						}
					}
					EventName eventNameForApp = EventName.getEventByIndex(eventName.getPublishedIndex());
					EventReason eventReason = EventReason.getEventByXioIndex(reason);
					try {
						callbacks.onSessionEvent(eventNameForApp, eventReason);
					} catch (Exception e) {
						eqh.setCaughtException(e);
						LOG.debug(this.toLogString() + "[onSessionEvent] Callback exception occurred. Event was " + eventName.toString());
					}
					return true;
				}
				break;

			case 6: // on new session
				if (LOG.isDebugEnabled()) {
					LOG.debug(this.toLogString() + "Received New Session Event");
				}
				if (ev instanceof EventNewSession) {
					long ptrSes = ((EventNewSession) ev).getPtrSes();
					if (ptrSes == 0) {
						throw new RuntimeException("malloc of class in C failed");
					}
					String uri = ((EventNewSession) ev).getUri();
					String srcIP = ((EventNewSession) ev).getSrcIP();
					Worker workerHint = null;
					String[] arr = uri.split(WorkerCache.CACHE_TAG+"=");
					if (arr.length > 1 && cache != null) {
						String key = srcIP+"|"+arr[1];
						workerHint = cache.getCachedWorker(key);
					}
					String uriStr = arr[0].substring(0,arr[0].length() - 1);
					ServerSession.SessionKey sesKey = new ServerSession.SessionKey(ptrSes, uriStr);
					try {
						this.callbacks.onSessionNew(sesKey, srcIP, workerHint);
					} catch (Exception e) {
						eqh.setCaughtException(e);
						LOG.debug(this.toLogString() + "[onSessionNew] Callback exception occurred. Session Key was " + sesKey.toString() + " and source IP was " + srcIP);
					}
					return true;
				}
				break;

			default:
				break;
		}
		LOG.error(this.toLogString() + "Received an un-handled event " + ev.getEventType());
		return false;
	}

	void removeSession(ServerSession s) {
		this.sessions.remove(s);
	}

	private URI replacePortInURI(URI uri, int newPort) {
		URI newUri = null;
		try {
			newUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), newPort, uri.getPath(), uri.getQuery(),
			        uri.getFragment());
			if (LOG.isDebugEnabled()) {
				LOG.debug(this.toLogString() + "uri with port " + newPort + " is " + newUri.toString());
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
			LOG.error(this.toLogString() + "URISyntaxException occured while trying to create a new URI");
		}

		return newUri;
	}

	private URI replaceIPinURI(URI uriForForward, String uriIPAddress) {
		URI newUri = null;
		try {
			newUri = new URI(uriForForward.getScheme(), uriForForward.getUserInfo(), new URI(uriIPAddress).getHost(),
 uriForForward.getPort(),
			        uriForForward.getPath(), uriForForward.getQuery(), uriForForward.getFragment());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			LOG.error(this.toLogString() + "URISyntaxException occured while trying to create a new URI");
		}
		return newUri;
	}
	
	boolean canClose() {
		return true;
	}

	public String toString() {
		return this.name;
	}

	private String toLogString() {
		return this.nameForLog;
	}
}
