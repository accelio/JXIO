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

public class ServerPortal extends EventQueueHandler.Eventable {

	private final Callbacks         callbacks;
	private final EventQueueHandler eventQHndl;
	private String                  uri;
	private URI                     uriPort0;
	private final int               port;
	private static final Log        LOG = LogFactory.getLog(ServerPortal.class.getCanonicalName());

	public static interface Callbacks {
		public void onSessionNew(long ptrSes, String uri, String srcIP);

		public void onSessionEvent(EventName session_event, EventReason reason);
	}

	/*
	 * this c-tor is for the ServerPortal manager. He listens on a well known port and redirects the request for a new
	 * session to ServerPortal worker
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

		if (getId() == 0) {
			LOG.fatal("there was an error creating ServerPortal");
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("id as recieved from C is " + getId());
		}
		this.uriPort0 = replacePortInsideURI(uri, 0);
		this.uri = replacePortInsideURI(uri, this.port).toString();

		this.eventQHndl.addEventable(this);
	}

	/*
	 * this c-tor is used for ServerPortal worker. a new session is redirected here by ServerPortal manager
	 */
	public ServerPortal(EventQueueHandler eventQHandler, URI uri) {
		this(eventQHandler, uri, null);
	}

	public URI getUriForServer() {
		return uriPort0;
	}

	public boolean close() {
		this.eventQHndl.removeEventable(this); // TODO: fix this
		if (getId() == 0) {
			LOG.error("closing ServerPortal with empty id");
			return false;
		}
		Bridge.stopServerPortal(getId());
		setIsClosing(true);
		return true;
	}

	public void accept(ServerSession serverSession) {
		serverSession.setEventQueueHandlers(this.eventQHndl, this.eventQHndl);
		Bridge.acceptSession(serverSession.getId());
	}

	public void forward(ServerPortal portal, ServerSession serverSession) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("portal " + portal + " ses id is " + serverSession.getId());
		}
		if (portal == this) {// in case forward was called but the user really means accept
			accept(serverSession);
			return;
		}
		serverSession.setEventQueueHandlers(this.eventQHndl, portal.eventQHndl);
		Bridge.forwardSession(portal.getUri(), serverSession.getId());
	}

	boolean getIsExpectingEventAfterClose() {
		return false;
	}

	void onEvent(Event ev) {
		switch (ev.getEventType()) {

			case 0: // session error event
				if (LOG.isDebugEnabled()) {
					LOG.debug("received session error event");
				}
				if (ev instanceof EventSession) {
					int errorType = ((EventSession) ev).getErrorType();
					int reason = ((EventSession) ev).getReason();
					callbacks.onSessionEvent(EventName.getEventByIndex(errorType), EventReason.getEventByIndex(reason));
			
					if (errorType == 1) {// event = "SESSION_TEARDOWN";
						this.eventQHndl.removeEventable(this); // now we are officially done with this session and it
						                                       // can be deleted from the EQH
					}
				}
				break;

			case 5: // on new session
				if (LOG.isDebugEnabled()) {
					LOG.debug("received new session event");
				}
				if (ev instanceof EventNewSession) {
					long ptrSes = ((EventNewSession) ev).getPtrSes();
					String uri = ((EventNewSession) ev).getUri();
					String srcIP = ((EventNewSession) ev).getSrcIP();
					this.callbacks.onSessionNew(ptrSes, uri, srcIP);
				}
				break;

			default:
				LOG.error("received an unknown event " + ev.getEventType());
		}
	}

	private URI replacePortInsideURI(URI uri, int newPort) {
		URI newUri = null;
		try {
			newUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), newPort, uri.getPath(), uri.getQuery(),
			        uri.getFragment());
			LOG.debug("uri with port " + newPort + " is " + newUri.toString());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			LOG.error("URISyntaxException occured while trying to create a new URI");
		}
		
		return newUri;
	}

	private String getUri() {
		return uri;
	}
}
