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
package com.mellanox.jxio;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.impl.Bridge;
import com.mellanox.jxio.impl.Event;
import com.mellanox.jxio.impl.EventNewSession;
import com.mellanox.jxio.impl.EventSession;

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
 * Callbacks is invoked. User must implement this interface and pass it in c-tor.
 * The events are:
 * 1. onSessionNew.
 * 2. onSessionEvent.
 * 
 */
public class ServerPortal extends EventQueueHandler.Eventable {

	private final Callbacks         callbacks;
	private final EventQueueHandler eventQHndl;
	private URI                     uri;
	private URI                     uriPort0;
	private final int               port;
	private Set<ServerSession>      sessions = new HashSet<ServerSession>();
	private final String            name;
	private final String            nameForLog;
	private static final Log        LOG      = LogFactory.getLog(ServerPortal.class.getCanonicalName());

	public static interface Callbacks {

		/**
		 * This event is triggered when a request for a new session arrives from Client.
		 * 
		 * @param sesKey
		 *            - SessionKey. Contains long (id of the session) and String (uri of the session)
		 *            Needs to be passed to ServerSession c-tor.
		 * @param srcIP
		 *            - IP of the Client
		 */
		public void onSessionNew(ServerSession.SessionKey sesKey, String srcIP);

		/**
		 * This event is triggered when PORTAL_CLOSED event arrives
		 * 
		 * @param session_event
		 *            - the event that was triggered
		 * @param reason
		 *            - the object containing the reason for triggerring session_event
		 */
		public void onSessionEvent(EventName session_event, EventReason reason);
	}

	/**
	 * This constructor is for the ServerPortal listener. He istens on a well known port and redirects
	 * the request for a new session to ServerPortal worker
	 * 
	 * @param eventQHandler
	 *            - EventQueueHAndler on which the events
	 *            (onSessionNew, onSessionEvent etc) of this portal will arrive
	 * @param uri
	 *            - on which the ServerPortal will listen. Should contain a well known port
	 * @param callbacks
	 *            - implementation of Interface ServerPortal.Callbacks
	 */
	public ServerPortal(EventQueueHandler eventQHandler, URI uri, Callbacks callbacks) {
		this.eventQHndl = eventQHandler;
		this.callbacks = callbacks;

		if (!uri.getScheme().equals("rdma")) {
			LOG.fatal("mal formatted URI: " + uri);
		}

		long[] ar = Bridge.startServerPortal(uri.toString(), eventQHandler.getId());
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

		this.eventQHndl.addEventable(this);
	}

	/**
	 * This constructor is for the ServerPortal worker. A new session is redirected here by ServerPortal listener
	 * 
	 * @param eventQHandler
	 *            - EventQueueHandler on which events of the session will arrive
	 * @param uri
	 *            - Should be uri for listener from ServerPortal listener; listener.getUriForServer()
	 */
	public ServerPortal(EventQueueHandler eventQHandler, URI uri) {
		this(eventQHandler, uri, null);
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
		serverSession.setEventQueueHandlers(this.eventQHndl, this.eventQHndl);
		long ptrSesServer = Bridge.acceptSession(serverSession.getId(), this.getId());
		serverSession.setPtrServerSession(ptrSesServer);
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
			uriForForward = this.replaceIPinURI(uriForForward, serverSession.uri);
		}

		serverSession.setEventQueueHandlers(this.eventQHndl, portal.eventQHndl);
		long ptrSesServer = Bridge.forwardSession(uriForForward.toString(), serverSession.getId(), portal.getId(), this.getId());
		serverSession.setPtrServerSession(ptrSesServer);
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

	private void setSession(ServerSession serverSession) {
		this.sessions.add(serverSession);
		serverSession.setPortal(this);
	}

	void onEvent(Event ev) {
		switch (ev.getEventType()) {

			case 0: // session error event
				if (LOG.isDebugEnabled()) {
					LOG.debug(this.toLogString() + "received session error event");
				}
				if (ev instanceof EventSession) {
					int errorType = ((EventSession) ev).getErrorType();
					int reason = ((EventSession) ev).getReason();
					EventName eventName = EventName.getEventByIndex(errorType);
					if (eventName == EventName.SESSION_CLOSED) {
						this.eventQHndl.removeEventable(this); // now we are officially done with this session and it
						                                       // can be deleted from the EQH
					}
					if (eventName == EventName.PORTAL_CLOSED) {
						this.eventQHndl.removeEventable(this);
						if (LOG.isDebugEnabled()) {
							LOG.debug(this.toLogString() + "portal was closed");
						}
					}
					callbacks.onSessionEvent(eventName, EventReason.getEventByIndex(reason));
				}
				break;

			case 6: // on new session
				if (LOG.isDebugEnabled()) {
					LOG.debug(this.toLogString() + "received new session event");
				}
				if (ev instanceof EventNewSession) {
					long ptrSes = ((EventNewSession) ev).getPtrSes();
					String uri = ((EventNewSession) ev).getUri();
					String srcIP = ((EventNewSession) ev).getSrcIP();
					ServerSession.SessionKey sesKey = new ServerSession.SessionKey(ptrSes, uri);
					this.callbacks.onSessionNew(sesKey, srcIP);
				}
				break;

			default:
				LOG.error(this.toLogString() + "received an unknown event " + ev.getEventType());
		}
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
			        uriForForward.getPort(), uriForForward.getPath(), uriForForward.getQuery(),
			        uriForForward.getFragment());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			LOG.error(this.toLogString() + "URISyntaxException occured while trying to create a new URI");
		}
		return newUri;
	}

	private URI getUri() {
		return uri;
	}

	public String toString() {
		return this.name;
	}

	private String toLogString() {
		return this.nameForLog;
	}
}
