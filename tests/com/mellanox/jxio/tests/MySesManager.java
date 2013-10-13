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

package com.mellanox.jxio.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.*;

public class MySesManager  {

	private final static Log LOG = LogFactory.getLog(MySesManager.class.getCanonicalName());
	EventQueueHandler eqh = null;
	ServerManager serverManager;

	public MySesManager(EventQueueHandler eqh, String url) {
		this.eqh = eqh;	
		this.serverManager = new ServerManager (eqh, url, new MySesManagerCallbacks());
	}

	public void close() {
		serverManager.close();
	}

	class MySesManagerCallbacks implements ServerManager.Callbacks {

		public void onSession(long ptrSes, String uriSrc, String srcIP) {		
			EventQueueHandler eventQHndl = new EventQueueHandler();	
			MsgPool msgPool = new MsgPool(1, 4, 4);
			eventQHndl.bindMsgPool (msgPool);
			MySesServerCallbacks c = new MySesServerCallbacks();
			ServerSession ses = new ServerSession(eventQHndl, serverManager.getUrlForServer(), c);
			c.serverSession = ses;
			serverManager.forward(ses, ptrSes);

			Thread t = new Thread (eventQHndl);
			t.start();	
		}

		public void onSessionError(int session_event, String reason) {
			String event;
			switch (session_event){
			case 0:
				event = "SESSION_REJECT";
				serverManager.close(); // Added
				break;
			case 1:
				event = "SESSION_TEARDOWN";
				serverManager.close(); // Added
				break;
			case 2:
				event = "CONNECTION_CLOSED";
				serverManager.close(); // Added
				break;
			case 3:
				event = "CONNECTION_ERROR";
				serverManager.close(); // Added
				break;
			case 4:
				event = "SESSION_ERROR";
				serverManager.close(); // Added
				break;
			default:
				event = "UNKNOWN";
				serverManager.close(); // Added
				break;
			}
			LOG.error("GOT EVENT " + event + "because of " + reason);
		}
	}
}
