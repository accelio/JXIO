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
import java.net.*;

import com.mellanox.jxio.*;
import com.mellanox.jxio.EventName;

public class MySesPortalTest {

	private final static Log LOG = LogFactory.getLog(MySesPortalTest.class.getCanonicalName());
	EventQueueHandler        eqh1, eqh2;
	ServerPortal             man, worker;

	MySesPortalTest(URI uri) {
		eqh1 = new EventQueueHandler();
		MyPortalManagerCallbacks c = new MyPortalManagerCallbacks();
		this.man = new ServerPortal(eqh1, uri, c);

		Thread t = new Thread(eqh1);
		t.start();

		eqh2 = new EventQueueHandler();
		MsgPool msgPool = new MsgPool(1, 4, 4);
		eqh2.bindMsgPool(msgPool);
		this.worker = new ServerPortal(eqh2, man.getUriForServer());
		Thread t2 = new Thread(eqh2);
		t2.start();
	}

	public static void main(String[] args) {
		String uriString = args[0];
		String port = args[1];
		String combined_uri = "rdma://" + uriString + ":" + port;

		URI uri = null;
	    try {
	    	uri = new URI(combined_uri);
	    } catch (URISyntaxException e) {
	    	// TODO Auto-generated catch block
	    	e.printStackTrace();
	    }

		MySesPortalTest spt = new MySesPortalTest(uri);
	}

	public void close() {
		// serverManager.close();
	}

	class MyPortalManagerCallbacks implements ServerPortal.Callbacks {
		// ServerPortal man;

		public void onSessionNew(long ptrSes, String uriSrc, String srcIP) {
			MySesServerCallbacks c = new MySesServerCallbacks();
			ServerSession ses = new ServerSession(ptrSes, c);
			c.serverSession = ses;
			LOG.debug("****************************" + man);
			man.forward(worker, ses);
		}

        public void onSessionEvent(EventName session_event, EventReason reason) {
			LOG.error("GOT EVENT " + session_event.toString() + "because of " + reason.toString());
			}

	}
}
