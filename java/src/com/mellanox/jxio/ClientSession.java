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
import java.net.URI;

import com.mellanox.jxio.impl.Bridge;
import com.mellanox.jxio.impl.Event;
import com.mellanox.jxio.impl.EventNewMsg;
import com.mellanox.jxio.impl.EventSession;

public class ClientSession extends EventQueueHandler.Eventable {

	private final Callbacks         callbacks;
	private final EventQueueHandler eventQHandler;
	private static final Log        LOG = LogFactory.getLog(ClientSession.class.getCanonicalName());

	public static interface Callbacks {
		public void onReply(Msg msg);

		public void onSessionEstablished();

		public void onSessionEvent(EventName session_event, String reason);

		public void onMsgError();
	}

	public ClientSession(EventQueueHandler eventQHandler, URI uri, Callbacks callbacks) {
		this.eventQHandler = eventQHandler;
		this.callbacks = callbacks;
		if (!uri.getScheme().equals(new String("rdma"))) {
			LOG.fatal("mal formatted URI: " + uri);
		}

		final long id = Bridge.startSessionClient(uri.toString(), eventQHandler.getId());
		if (id == 0) {
			LOG.error("there was an error creating session");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("id as recieved from C is " + id);
		}
		this.setId(id);

		this.eventQHandler.addEventable(this);
	}

	public boolean sendMessage(Msg msg) {
		msg.setClientSession(this);
		eventQHandler.addMsgInUse(msg);
		boolean ret = Bridge.clientSendReq(this.getId(), msg.getId());
		if (!ret) {
			LOG.error("there was an error sending the message");
		}
		return ret;
	}

	public boolean close() {
		if (getId() == 0) {
			LOG.error("closing Session with empty id");
			return false;
		}
		Bridge.closeSessionClient(getId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("at the end of SessionClientClose");
		}
		setIsClosing(true);
		return true;
	}

	boolean getIsExpectingEventAfterClose() {
		return true;
	}

	void onEvent(Event ev) {
		switch (ev.getEventType()) {

			case 0: // session error event
				if (LOG.isDebugEnabled()) {
					LOG.debug("received session event");
				}
				if (ev instanceof EventSession) {

					int errorType = ((EventSession) ev).getErrorType();
					String reason = ((EventSession) ev).getReason();
					callbacks.onSessionEvent(EventName.getEventByIndex(errorType), reason);

					if (errorType == 1) {// event = "SESSION_TEARDOWN";
						eventQHandler.removeEventable(this); // now we are officially done with this session and it can
						                                     // be deleted from the EQH
					}
				}
				break;

			case 1: // msg error
				LOG.error("received msg error event");
				callbacks.onMsgError();
				break;

			case 2: // session established
				if (LOG.isDebugEnabled()) {
					LOG.debug("received session established event");
				}
				callbacks.onSessionEstablished();
				break;

			case 4: // on reply
				if (LOG.isTraceEnabled()) {
					LOG.trace("received msg event");
				}
				Msg msg = ((EventNewMsg) ev).getMsg();
				callbacks.onReply(msg);
				break;

			default:
				LOG.error("received an unknown event " + ev.getEventType());
		}
	}
}
