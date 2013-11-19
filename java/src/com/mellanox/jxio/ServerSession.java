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
import com.mellanox.jxio.impl.EventNewMsg;
import com.mellanox.jxio.impl.EventSession;

public class ServerSession extends EventQueueHandler.Eventable {

	private final Callbacks callbacks;
	private EventQueueHandler eventQHandler;
	private static final Log  LOG    = LogFactory.getLog(ServerSession.class.getCanonicalName());

	public static interface Callbacks {
		public void onRequest(Msg msg);

		public void onSessionEvent(EventName session_event, String reason);

		public void onMsgError();
	}

	public ServerSession(long sessionKey, Callbacks callbacks) {
		this.callbacks = callbacks;
		setId(sessionKey);
		if (LOG.isDebugEnabled()) {
			LOG.debug("id as recieved from C is " + getId());
		}
	}

	public boolean close() {
		eventQHandler.removeEventable(this); //TODO: fix this
		if (getId() == 0){
			LOG.error("closing ServerSession with empty id");
			return false;
		}
		Bridge.closeServerSession(getId());
		setIsClosing(true);
		return true;
	}

	public boolean sendResponce(Msg msg) {
		boolean ret = Bridge.serverSendReply(msg.getId());
		if (!ret){
			LOG.error( "there was an error sending the message");
		}
		this.eventQHandler.releaseMsgBackToPool(msg); 
		/*
	    this message shold be released back to pool.
	    even though the message might not reached the client yet, it's ok since this pool is 
	    used only for matching of id to object. the actual release to pool is done on c side
		 */
		return ret;
	}

	void setEventQueueHandler(EventQueueHandler eqh){
		this.eventQHandler = eqh;
		this.eventQHandler.addEventable(this);
	}

	void onEvent(Event ev) {
		switch (ev.getEventType()) {
			case 0: //session error event
				LOG.error("received session error event");
				if (ev  instanceof EventSession) {
					int errorType = ((EventSession) ev).getErrorType();
					String reason = ((EventSession) ev).getReason();
					callbacks.onSessionEvent(EventName.getEventByIndex(errorType), reason);

					if (errorType == 1) {// event = "SESSION_TEARDOWN";
						eventQHandler.removeEventable(this); // now we are officially done with this session and it can
															 // be deleted from the EQH
					}
				}
				break;

			case 1: //msg error
				LOG.error("received msg error event");
				callbacks.onMsgError();
				break;

			case 3: //on request
				if(LOG.isTraceEnabled()) {
					LOG.trace("received msg event");
				}
				Msg msg = ((EventNewMsg) ev).getMsg();
				callbacks.onRequest(msg);
				break;

			case 6: //msg sent complete
				if(LOG.isTraceEnabled()) {
					LOG.trace("received msg sent complete event");
				}
				break;

			default:
				LOG.error("received an unknown event "+ ev.getEventType());	
		}
	}

}
