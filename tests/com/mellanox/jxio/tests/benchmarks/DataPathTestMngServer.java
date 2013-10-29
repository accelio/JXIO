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

import com.mellanox.jxio.*;

public class DataPathTestMngServer implements Runnable {

	private final static Log LOG = LogFactory.getLog(DataPathTestMngServer.class.getCanonicalName());
	EventQueueHandler eqh = null;
	ServerManager serverManager;
	int msgSize;

	public DataPathTestMngServer(String url, int msgSize) {
		this.eqh = new EventQueueHandler();	
		this.serverManager = new ServerManager (eqh, url, new SesManagerCallbacks());
		this.msgSize = msgSize;
	}

	public void close() {
		serverManager.close();
	}

	public void run() {		
		eqh.runEventLoop(-1, -1);
	}
	
	public void print(String str) {
		LOG.debug("********" + str);
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("StatMain $IP $PORT $MSG_SIZE");
			return;
		}
		String url = "rdma://" + args[0] + ":" + Integer.parseInt(args[1]);
		Runnable test = new DataPathTestMngServer(url, Integer.parseInt(args[2]));
		test.run();
	}
	
	class SesManagerCallbacks implements ServerManager.Callbacks {

		public void onSession(long ptrSes, String uriSrc, String srcIP) {		
			EventQueueHandler eventQHndl = new EventQueueHandler();	
			MsgPool msgPool = new MsgPool(2048, msgSize, msgSize);
			eventQHndl.bindMsgPool(msgPool);
			DataPathTestServer server = new DataPathTestServer(eventQHndl, serverManager.getUrlForServer());
			
			serverManager.forward(server.session, ptrSes);

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
