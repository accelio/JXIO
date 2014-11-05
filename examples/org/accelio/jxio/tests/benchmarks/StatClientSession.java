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
package org.accelio.jxio.tests.benchmarks;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.Msg;
import org.accelio.jxio.EventName;
import org.accelio.jxio.EventReason;
import org.accelio.jxio.ClientSession;
import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.tests.benchmarks.StatTest;

public class StatClientSession {
	private final static Log LOG = LogFactory.getLog(StatTest.class.getCanonicalName());

	EventQueueHandler        eqh = null;
	int                      clients_count;
	ClientSession            clients[];

	public StatClientSession(EventQueueHandler eqh, String uriString, int num_clients) {
		this.clients_count = 0;
		this.eqh = eqh;
		this.clients = new ClientSession[num_clients];
		
		URI uri = null;
	    try {
	    	uri = new URI(uriString);
	    } catch (URISyntaxException e) {
	    	// TODO Auto-generated catch block
	    	e.printStackTrace();
	    }
		
		for (int i = 0; i < num_clients; i++) {
			this.clients[i] = new ClientSession(eqh, uri, new StatSesClientCallbacks(i));
		}
	}

	public void close() {
		for (ClientSession cs : clients) {
			cs.close();
		}
	}

	public void print(String str) {
		LOG.debug("********" + str);
	}

	class StatSesClientCallbacks implements ClientSession.Callbacks {
		int session_num;

		public StatSesClientCallbacks(int index) {
			this.session_num = index;
		}

		public void onMsgError() {
			// logger.log(Level.debug, "onMsgErrorCallback");
			print("onMsgErrorCallback");
		}

		public void onSessionEstablished() {
			// logger.log(Level.debug, "[SUCCESS] Session Established! Bring the champagne!");
			print(session_num + " Session Established");
			clients[session_num].close();
		}

		public void onSessionEvent(EventName event, EventReason reason) {
			if (event == EventName.SESSION_TEARDOWN) {
				print(session_num + " Session Teardown");
				clients_count++;
				if (clients_count == clients.length) {
					print(session_num + " Stopping Event Loop");
					eqh.breakEventLoop();
				}
			}
			print(session_num + "GOT EVENT " + event.toString() + " because of " + reason.toString());
		}

		public void onResponse(Msg msg) {

		}
	}
}
