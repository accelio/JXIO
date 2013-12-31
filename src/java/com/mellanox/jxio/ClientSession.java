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
	private static final Log        LOG           = LogFactory.getLog(ClientSession.class.getCanonicalName());

	public static interface Callbacks {
		public void onReply(Msg msg);

		public void onSessionEstablished();

		public void onSessionEvent(EventName session_event, EventReason reason);

		public void onMsgError();
	}

	public ClientSession(EventQueueHandler eventQHandler, URI uri, Callbacks callbacks) {
		this.eventQHandler = eventQHandler;
		this.callbacks = callbacks;
		if (!uri.getScheme().equals("rdma")) {
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
		if (this.getIsClosing()) {
			LOG.warn("Trying to send message while session is closing");
			return false;
		}
		if (!Bridge.clientSendReq(this.getId(), msg.getId(), msg.getOut().position())) {
			LOG.error("there was an error sending the message");
			return false;
		}
		msg.setClientSession(this);
		//only if the send was successful the msg needs to be added to the "pending reply" list
		eventQHandler.addMsgInUse(msg);
		return true;
	}

	public boolean close() {
		if (this.getIsClosing()) {
			LOG.warn("attempting to close client that is already closed or being closed");
			return false;
		}
		if (getId() == 0) {
			LOG.error("closing Session with empty id");
			return false;
		}
		setIsClosing(true);
		
		Bridge.closeSessionClient(getId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("at the end of SessionClientClose" + this.toString());
		}
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
					int reason = ((EventSession) ev).getReason();
					EventName eventName = EventName.getEventByIndex(errorType);
					if (eventName == EventName.SESSION_TEARDOWN || eventName == EventName.SESSION_REJECT) {
						eventQHandler.removeEventable(this); // now we are officially done with this session and it can
						this.setIsClosing(true);   // be deleted from the EQH
						Bridge.deleteClient(this.getId());
					}
					callbacks.onSessionEvent(eventName, EventReason.getEventByIndex(reason));
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
					LOG.trace("received msg event at client" + this.toString());
				}
				EventNewMsg evNewMsg;
				if (ev instanceof EventNewMsg) {
					evNewMsg = (EventNewMsg) ev;
					Msg msg = evNewMsg.getMsg();
					callbacks.onReply(msg);
				} else {
					LOG.error("Event is not an instance of EventNewMsg" + this.toString());
				}

				break;

			default:
				LOG.error("received an unknown event " + ev.getEventType());
		}
	}
}
