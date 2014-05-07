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

package com.mellanox.jxio.tests.controlPathDuration;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.ServerPortal;

public class CPDServerPortalWorker extends Thread {

	private final ServerPortal      sp;
	private final EventQueueHandler eqh;
	private final int               portal_index;
	private AtomicInteger           num_of_sessions;
	private final static Log        LOG = LogFactory.getLog(CPDServerPortalWorker.class.getCanonicalName());

	public CPDServerPortalWorker(int index, URI uri) {
		portal_index = index;
		eqh = new EventQueueHandler(null);
		sp = new ServerPortal(eqh, uri);
	}

	public void run() {
		eqh.run();
	}

	public ServerPortal getPortal() {
		return sp;
	}

}