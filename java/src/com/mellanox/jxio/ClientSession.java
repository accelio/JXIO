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

import java.util.logging.Level;

import com.mellanox.jxio.impl.Bridge;
import com.mellanox.jxio.impl.Event;
import com.mellanox.jxio.impl.EventSession;
import com.mellanox.jxio.impl.Eventable;


public class ClientSession implements Eventable{

	
	private long id = 0;
	protected EventQueueHandler eventQHandler = null;
	protected String url;
	boolean isClosing = false; //indicates that this class is in the process of releasing it's resources
	private ClientSessionCallbacks callbacks;
	

	
	private static Log logger = Log.getLog(ClientSession.class.getCanonicalName());
	
	public ClientSession(EventQueueHandler eventQHandler, String url, ClientSessionCallbacks callbacks) {
		this.eventQHandler = eventQHandler;
		this.url = url;
		this.callbacks = callbacks;
		
		this.id = Bridge.startSessionClient(url, eventQHandler.getID());
		if (this.id == 0){
			logger.log(Level.SEVERE, "there was an error creating session");
		}
		logger.log(Level.INFO, "id is "+id);
		
		this.eventQHandler.addEventable (this); 
	}
	
	
	public void onEvent(Event ev) {
		switch (ev.getEventType()) {

		case 0: //session error event
			logger.log(Level.INFO, "received session event");
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

		case 2: //session established
			logger.log(Level.INFO, "received session established event");
			callbacks.onSessionEstablished();
			break;
			
		case 3: //on reply
			logger.log(Level.INFO, "received msg event");
			Msg msg = null;
			callbacks.onReply(msg);//this is obviuosly temporary implementation


			break;
			
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ ev.getEventType());
		}
	}
	
	
	public long getId(){ return id;}
	
	public boolean isClosing() {return isClosing;}
	
	public boolean sendMessage (Msg msg){
	    msg.setClientSession(this);
	    eventQHandler.addMsgInUse(msg);
	    boolean ret = Bridge.sendMsg(this.getId(), 0, msg.getId());
	    if (!ret){
		logger.log(Level.SEVERE, "there was an error sending the message");
	    }
	    return ret;

	}
	
	
	public boolean close (){

		if (id == 0){
			logger.log(Level.SEVERE, "closing Session with empty id");
			return false;
		}
		Bridge.closeSessionClient(id);	
		
		logger.log(Level.INFO, "in the end of SessionClientClose");
		isClosing = true;
		return true;
	}
/*	
	public void closeSession(){
		//calls d-tor of cjxsession(which does nothing)
		Bridge.closeSessionClient(id);
	}
*/	
}
