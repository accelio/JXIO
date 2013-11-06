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

public class ServerPortal extends EventQueueHandler.Eventable {

	private final Callbacks         callbacks;
	private final EventQueueHandler eventQHndl;
	private String                  url;
	private String                  urlPort0;
	private final int               port;
	private static final Log        LOG = LogFactory.getLog(ServerPortal.class.getCanonicalName());

	public static interface Callbacks {
		public void onSessionNew(long ptrSes, String uri, String srcIP);

		public void onSessionEvent(int errorType, String reason);
	}

	/*
	 * this c-tor is for the ServerPortal manager. He listens on a well known port and redirects the request for a new
	 * session to ServerPortal worker
	 */
	public ServerPortal(EventQueueHandler eventQHandler, String url, Callbacks callbacks) {
		this.eventQHndl = eventQHandler;
		this.callbacks = callbacks;

		long[] ar = Bridge.startServerPortal(url, eventQHandler.getId());
		this.setId(ar[0]);
		this.port = (int) ar[1];

		if (getId() == 0) {
			LOG.fatal("there was an error creating ServerPortal");
		}

		createUrlForServerSession(url);
		// modify url to include the new port number
		int index = url.lastIndexOf(":");
		this.url = url.substring(0, index + 1) + Integer.toString(port);
		if (LOG.isDebugEnabled()) {
			LOG.debug("****** new url is " + this.url);
		}
		this.eventQHndl.addEventable(this);
	}

	/*
	 * this c-tor is used for ServerPortal worker. a new session is redirected here by ServerPortal manager
	 */
	public ServerPortal(EventQueueHandler eventQHandler, String url) {
		this(eventQHandler, url, null);
	}

	public String getUrlForServer() {
		return urlPort0;
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

	public void accept(ServerSession ses) {
		this.forward(this, ses);
	}

	public void forward(ServerPortal portal, ServerSession serverSession) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("portal " + portal + "ses id is " + serverSession.getId());
		}
		serverSession.setEventQueueHandler(portal.eventQHndl);
		Bridge.forwardSession(portal.getUrl(), serverSession.getId(), portal.getId());
	}

	void onEvent(Event ev) {
		switch (ev.getEventType()) {

			case 0: // session error event
				if (LOG.isDebugEnabled()) {
					LOG.debug("received session error event");
				}
				if (ev instanceof EventSession) {
					int errorType = ((EventSession) ev).getErrorType();
					String reason = ((EventSession) ev).getReason();
					this.callbacks.onSessionEvent(errorType, reason);

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

	private void createUrlForServerSession(String url) {
		// parse url so it would replace port number on which the server listens with 0
		int index = url.lastIndexOf(":");
		this.urlPort0 = url.substring(0, index + 1) + "0";
		LOG.debug("urlForServerSession is " + urlPort0);
	}

	private String getUrl() {
		return url;
	}
}
