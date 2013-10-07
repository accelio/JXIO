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

import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.mellanox.jxio.impl.Bridge;
import com.mellanox.jxio.impl.Event;
import com.mellanox.jxio.impl.EventNewMsg;
import com.mellanox.jxio.impl.EventSession;

public class ServerSession extends EventQueueHandler.Eventable {
	
	private final Callbacks callbacks;
	private final EventQueueHandler eventQHandler;
	private final int port;
	private String url;

	private static Log logger = Log.getLog(ServerSession.class.getCanonicalName());

	public static interface Callbacks {
	    public void onRequest(Msg msg);
	    public void onSessionError(int session_event, String reason );
	    public void onMsgError();
	}

	public ServerSession(EventQueueHandler eventQHandler, String url, Callbacks callbacks) {
		this.eventQHandler = eventQHandler;
		this.url = url;
		this.callbacks = callbacks;
		logger.log(Level.INFO, "uri inside ServerSession is "+url);
		long[] ar = Bridge.startServer(url, eventQHandler.getId());
		final long id = ar[0];
		this.port = (int) ar[1];
		if (id == 0){
			logger.log(Level.SEVERE, "there was an error creating ServerSession");
		}
		logger.log(Level.INFO, "id is " + id);
		this.setId(id);

		//modify url to include the new port number
		int index = url.lastIndexOf(":"); 

		this.url = url.substring(0, index+1)+Integer.toString(port);
		logger.log(Level.INFO, "****** new url is " + this.url);
		this.eventQHandler.addEventable(this);
	}
	
	public boolean close() {
//		eventQHandler.removeEventable (this); //TODO: fix this
		if (getId() == 0){
			logger.log(Level.SEVERE, "closing ServerSession with empty id");
			return false;
		}
		Bridge.stopServer(getId());
		setIsClosing(true);
		return true;
	}
	
	public boolean sendResponce(Msg msg) {//obviously this is temporary implementation
	    return true;
	}

	void onEvent(Event ev) {
		switch (ev.getEventType()) {
		case 0: //session error event
			logger.log(Level.INFO, "received session error event");
			if (ev  instanceof EventSession) {

				int errorType = ((EventSession) ev).getErrorType();
				String reason = ((EventSession) ev).getReason();
				callbacks.onSessionError(errorType, reason);

				if (errorType == 1) {//event = "SESSION_TEARDOWN";
					eventQHandler.removeEventable(this); //now we are officially done with this session and it can be deleted from the EQH
				}
			}
			break;
			
		case 1: //msg error
			logger.log(Level.INFO, "received msg error event");
			callbacks.onMsgError();
			break;

		case 3: //on request
			logger.log(Level.INFO, "received msg event");
			Msg msg = ((EventNewMsg) ev).getMsg();
			callbacks.onRequest(msg);
			break;
		
		case 6: //msg sent complete
			logger.log(Level.INFO, "received msg sent complete event");
			break;
			
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ ev.getEventType());	
		}
		
	}

	protected final String getUrl() {
		return url;
	}
}
