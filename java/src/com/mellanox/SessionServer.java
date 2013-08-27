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
package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;


public abstract class SessionServer implements Eventable {
	
	private long id = 0; //represents pointer to server struct
//	private long ptrEventQueue = 0;
	private int port;
	protected String url;
	private EventQueueHandler eventQHandler  =null;
	boolean isClosing = false; //indicates that this class is in the process of releasing it's resources
	
	abstract public void onRequestCallback();
	abstract public void onSessionErrorCallback(int session_event, String reason );
	abstract public void onMsgErrorCallback();
	
	private static JXLog logger = JXLog.getLog(SessionServer.class.getCanonicalName());

	protected SessionServer(EventQueueHandler eventQHandler, String url) {
		this.eventQHandler = eventQHandler;
		this.url = url;
		logger.log(Level.INFO, "uri inside SessionServer is "+url);
		long [] ar = JXBridge.startServer(url, eventQHandler.getID());
		this.id = ar [0];
		this.port = (int) ar[1];
		if (this.id == 0){
			logger.log(Level.SEVERE, "there was an error creating SessionServer");
		}
		this.eventQHandler.addEventable (this);
	}
	

	
	public void onEvent(int eventType, Event ev){
		switch (eventType){
		case 0: //session error event
			logger.log(Level.INFO, "received session error event");
			if (ev  instanceof EventSession){
				int errorType = ((EventSession) ev).errorType;
				String reason = ((EventSession) ev).reason;
				this.onSessionErrorCallback(errorType, reason);
				if (errorType == 1) {//event = "SESSION_TEARDOWN";
					eventQHandler.removeEventable(this); //now we are officially done with this session and it can be deleted from the EQH
				}
			}
			break;
			
		case 1: //msg error
			logger.log(Level.INFO, "received msg error event");
			this.onMsgErrorCallback();
			break;

		case 3: //on request
			logger.log(Level.INFO, "received msg event");
			this.onRequestCallback();
			break;
		
		case 5: //msg sent complete
			logger.log(Level.INFO, "received msg sent complete event");
			break;
			
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);	
		}
		
	}
	
	public boolean close(){
//		eventQHandler.removeEventable (this); //TODO: fix this
		if (id == 0){
			logger.log(Level.SEVERE, "closing SessionServer with empty id");
			return false;
		}
		JXBridge.stopServer(id);
		isClosing = true;
		return true;
	}
	
	public long getId(){ return id;}
	
	public boolean isClosing() {return isClosing;}

}
