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
package org.accelio.jxio.tests.benchmarks.jxioConnection;

import java.net.URI;
import java.net.URISyntaxException;

import org.accelio.jxio.jxioConnection.JxioConnectionServer;
import org.accelio.jxio.tests.benchmarks.jxioConnection.UserServerCallbacks;

public class StreamServer {

	public static void main(String[] args) {

		try {
			String uriString = String.format("rdma://%s:%s", args[0], args[1]);
			URI uri = null;
			try {
				uri = new URI(uriString);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			JxioConnectionServer conServer = new JxioConnectionServer(uri, Integer.parseInt(args[2]),
			        new UserServerCallbacks());
			conServer.start();
		} catch (Throwable t) {
			t.printStackTrace();
			t.getCause().printStackTrace();
		}
	}
}
