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
import com.mellanox.jxio.impl.EventSession;
import com.mellanox.jxio.impl.Eventable;

public class ServerSession implements Eventable {
	
	private long id = 0; //represents pointer to server struct
	private EventQueueHandler eventQHandler = null;
	private int port;
	protected String url;
	boolean isClosing = false; //indicates that this class is in the process of releasing it's resources
	private Callbacks callbacks;
	
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
		long[] ar = Bridge.startServer(url, eventQHandler.getID());
		this.id = ar[0];
		this.port = (int) ar[1];
		if (this.id == 0){
			logger.log(Level.SEVERE, "there was an error creating ServerSession");
		}
		//modify url to include the new port number
		int index = url.lastIndexOf(":"); 


		this.url = url.substring(0, index+1)+Integer.toString(port);
		logger.log(Level.INFO, "****** new url is "+this.url);
		this.eventQHandler.addEventable (this);
	}
	
	public void onEvent(Event ev) {
		switch (ev.getEventType()) {
		case 0: //session error event
			logger.log(Level.INFO, "received session error event");
			if (ev  instanceof EventSession){

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
			Msg msg = null; //obviously this is temporary implementation
			callbacks.onRequest(msg);
			break;
		
		case 5: //msg sent complete
			logger.log(Level.INFO, "received msg sent complete event");
			break;
			
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ ev.getEventType());	
		}
		
	}
	
	public boolean close() {
//		eventQHandler.removeEventable (this); //TODO: fix this
		if (id == 0){
			logger.log(Level.SEVERE, "closing ServerSession with empty id");
			return false;
		}
		Bridge.stopServer(id);
		isClosing = true;
		return true;
	}
	
	public long getId() { return id; }
	
	public boolean isClosing() { return isClosing; }
	
	public boolean sendResponce (Msg msg){//obviously this is temporary implementation
	    return true;
	}
}
