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
package com.mellanox.jxio.tests.benchmarks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.ServerSession;


public class DataPathTestServer {
	
	ServerSession session;

	private final static Log LOG = LogFactory.getLog(DataPathTestServer.class.getCanonicalName());
	
	public DataPathTestServer(long key) {
		this.session = new ServerSession (key, new ServerCallbacks());
	}
	
	public class ServerCallbacks implements ServerSession.Callbacks {

		public void onRequest(Msg msg) {

			session.sendResponce(msg);
		}

		public void onSessionEvent(int session_event, String reason) {
			String event;
			switch (session_event){
			case 0:
				event = "SESSION_REJECT";
				break;
			case 1:
				event = "SESSION_TEARDOWN";
				break;
			case 2:
				event = "CONNECTION_CLOSED";
				break;
			case 3:
				event = "CONNECTION_ERROR";
				break;
			case 4:
				event = "SESSION_ERROR";
				break;
			default:
				event = "UNKNOWN";
				break;
			}
			LOG.error("GOT EVENT " + event + "because of " + reason);
		}

		public void onMsgError() {
			LOG.info("onMsgErrorCallback");

		}
		
	}
}
