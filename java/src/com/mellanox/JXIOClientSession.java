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

import java.util.logging.Level;


public abstract class JXIOClientSession implements JXIOEventable{
	
	private long id = 0;
	protected JXIOEventQueueHandler eventQHandler  =null;
	protected String url;
//	protected int port;
	boolean isClosing = false; //indicates that this class is in the process of releasing it's resources
	
	abstract public void onReplyCallback();
	abstract public void onSessionEstablished();
	abstract public void onSessionErrorCallback(int session_event, String reason );
	abstract public void onMsgErrorCallback();

	
	private static JXIOLog logger = JXIOLog.getLog(JXIOClientSession.class.getCanonicalName());
	
	public JXIOClientSession(JXIOEventQueueHandler eventQHandler, String url){
		this.eventQHandler = eventQHandler;
		this.url = url;
		
		this.id = JXIOBridge.startSessionClient(url, eventQHandler.getID());
		if (this.id == 0){
			logger.log(Level.SEVERE, "there was an error creating session");
		}
		logger.log(Level.INFO, "id is "+id);
		
		this.eventQHandler.addEventable (this); 

	}
	
	
	public void onEvent(int eventType, JXIOEvent ev){
		
		switch (eventType){

		case 0: //session error event
			logger.log(Level.INFO, "received session event");
			if (ev  instanceof JXIOEventSession){
				int errorType = ((JXIOEventSession) ev).errorType;
				String reason = ((JXIOEventSession) ev).reason;
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

		case 2: //session established
			logger.log(Level.INFO, "received session established event");
			this.onSessionEstablished();
			break;
			
		case 3: //on reply
			logger.log(Level.INFO, "received msg event");
			this.onReplyCallback();
			break;
			
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
		}
		
	}
	
//	public boolean closeSession(){
//		return JXIOBridge.closeSessionClient(id);
//	}
	
	public long getId(){ return id;}
	
	public boolean isClosing() {return isClosing;}
	
	
	public boolean close (){

		if (id == 0){
			logger.log(Level.SEVERE, "closing Session with empty id");
			return false;
		}
		JXIOBridge.closeSessionClient(id);	
		
		logger.log(Level.INFO, "in the end of SessionClientClose");
		isClosing = true;
		return true;
	}
/*	
	public void closeSession(){
		//calls d-tor of cjxsession(which does nothing)
		JXIOBridge.closeSessionClient(id);
	}
*/	
}
