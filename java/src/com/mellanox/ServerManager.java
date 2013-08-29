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

public abstract class ServerManager implements Eventable{
	private EventQueueHandler eventQHndl = null;
	private long id = 0;
	private int port;
	private String url;
	private String urlPort0;
	static protected int sizeEventQ = 10000;
	boolean isClosing = false; //indicates that this class is in the process of releasing it's resources
	
	private static JXLog logger = JXLog.getLog(ServerManager.class.getCanonicalName());
	
	public abstract void onSession(long ptrSes, String uri, String srcIP);
	public abstract void onSessionError(int errorType, String reason);
	
	public ServerManager(EventQueueHandler eventQHandler,String url){
	    	this.url = url;
		eventQHndl = eventQHandler;
		
		long [] ar = JXBridge.startServer(url, eventQHandler.getID());
		this.id = ar [0];
		this.port = (int) ar[1];
		
		if (this.id == 0){
			logger.log(Level.SEVERE, "there was an error creating SessionManager");
		}
		createUrlForServerSession();
		logger.log(Level.INFO, "urlForServerSession is "+urlPort0);
		
		eventQHndl.addEventable (this); 
		eventQHndl.runEventLoop(1, 0);
	}
	
	private void createUrlForServerSession(){
	    //parse url so it would replace port number on which the server listens with 0
	    int index = url.lastIndexOf(":"); 
	    urlPort0 = url.substring(0, index+1)+"0";
	}
	
	
	public boolean close(){
		eventQHndl.removeEventable (this); //TODO: fix this
		if (id == 0){
			logger.log(Level.SEVERE, "closing ServerManager with empty id");
			return false;
		}
		JXBridge.stopServer(id);
		isClosing = true;
		return true;
	}
	
	public void forward(SessionServer ses, long ptrSes){
		JXBridge.forwardSession(ses.url, ptrSes, ses.getId());
	}
	
	public long getId(){ return id;} 
	public boolean isClosing() {return isClosing;}
	
	
	public void onEvent (int eventType, Event ev){
		switch (eventType){
		
		case 0: //session error event
			logger.log(Level.INFO, "received session error event");
			if (ev  instanceof EventSession){
				int errorType = ((EventSession) ev).errorType;
				String reason = ((EventSession) ev).reason;
				this.onSessionError(errorType, reason);
				if (errorType == 1) {//event = "SESSION_TEARDOWN";
					eventQHndl.removeEventable(this); //now we are officially done with this session and it can be deleted from the EQH
				}
			}
			break;
			
		case 4: //on new session
			logger.log(Level.INFO, "received session error event");
			if (ev  instanceof EventNewSession){
				long ptrSes = ((EventNewSession) ev).ptrSes;
				String uri = ((EventNewSession) ev).uri;		
				String srcIP = ((EventNewSession) ev).srcIP;
				this.onSession(ptrSes, uri, srcIP);
			}
			break;
		
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
		}
	}
	

}
