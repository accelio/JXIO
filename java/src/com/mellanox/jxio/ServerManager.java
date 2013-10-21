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

public class ServerManager extends EventQueueHandler.Eventable {

	private final Callbacks         callbacks;
	private final EventQueueHandler eventQHndl;
	private final String            url;
	private String                  urlPort0;
	private static final Log        LOG = LogFactory.getLog(ServerManager.class.getCanonicalName());

	public static interface Callbacks {
		public void onSession(long ptrSes, String uri, String srcIP);

		public void onSessionError(int errorType, String reason);
	}

	public ServerManager(EventQueueHandler eventQHandler, String url, Callbacks callbacks) {
		this.url = url;
		this.eventQHndl = eventQHandler;
		this.callbacks = callbacks;

		setId(Bridge.startServerManager(url, eventQHandler.getId()));

		if (getId() == 0) {
			LOG.fatal("there was an error creating SessionManager");
		}
		createUrlForServerSession();
		LOG.debug("urlForServerSession is " + urlPort0);

		this.eventQHndl.addEventable(this);
	}

	public String getUrlForServer() {
		return urlPort0;
	}

	public boolean close() {
		this.eventQHndl.removeEventable(this); // TODO: fix this
		if (getId() == 0) {
			LOG.error("closing ServerManager with empty id");
			return false;
		}
		Bridge.stopServerManager(getId());
		setIsClosing(true);
		return true;
	}

	public void forward(ServerSession ses, long ptrSes) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("****** new url inside forward  is " + ses.getUrl());
		}
		Bridge.forwardSession(ses.getUrl(), ptrSes, ses.getId());
	}

	void onEvent(Event ev) {
		switch (ev.getEventType()) {

			case 0: // session error event
				LOG.error("received session error event");
				if (ev instanceof EventSession) {
					int errorType = ((EventSession) ev).getErrorType();
					String reason = ((EventSession) ev).getReason();
					this.callbacks.onSessionError(errorType, reason);

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
					this.callbacks.onSession(ptrSes, uri, srcIP);
				}
				break;

			default:
				LOG.error("received an unknown event " + ev.getEventType());
		}
	}

	private void createUrlForServerSession() {
		// parse url so it would replace port number on which the server listens with 0
		int index = url.lastIndexOf(":");
		this.urlPort0 = url.substring(0, index + 1) + "0";
	}
}
