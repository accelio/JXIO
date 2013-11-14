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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.*;

public class DataPathTestMngServer implements Runnable {

	private final static Log LOG = LogFactory.getLog(DataPathTestMngServer.class.getCanonicalName());
	EventQueueHandler eqh1, eqh2; 
	ServerPortal man, worker;
	int msgSize;

	public DataPathTestMngServer(URI uri, int msgSize) {
		
		eqh1 = new EventQueueHandler(); 
		SesManagerCallbacks c = new SesManagerCallbacks();
		this.man = new ServerPortal (eqh1, uri, c);

		eqh2 = new EventQueueHandler(); 
		this.worker = new ServerPortal(eqh2, man.getUriForServer());

		this.msgSize = msgSize;
	}

	public void close() {
		man.close();
	}

	public void run() {	
		Thread t = new Thread (eqh1);
		t.start();	
		Thread t2 = new Thread (eqh2);
		t2.start();	
	}
	
	public void print(String str) {
		LOG.debug("********" + str);
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("StatMain $IP $PORT $MSG_SIZE");
			return;
		}
		
		String uriString = "rdma://" + args[0] + ":" + Integer.parseInt(args[1]);
		URI uri = null;
	    try {
	    	uri = new URI(uriString);
	    } catch (URISyntaxException e) {
	    	// TODO Auto-generated catch block
	    	e.printStackTrace();
	    }
		
		Runnable test = new DataPathTestMngServer(uri, Integer.parseInt(args[2]));
		test.run();
	}
	
	class SesManagerCallbacks implements ServerPortal.Callbacks {

		public void onSessionNew(long ptrSes, String uriSrc, String srcIP) {
			
			MsgPool msgPool = new MsgPool(2048, msgSize, msgSize);
			eqh2.bindMsgPool(msgPool);
			DataPathTestServer server = new DataPathTestServer(ptrSes);

			man.forward(worker, server.session);

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
	}
}
