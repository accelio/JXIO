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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class DataPathTest {

	// command line args
	protected int num_of_threads;
	protected int outMsg_size;
	protected int inMsg_size;
	protected int burst_size; 
	protected String test_type;
	protected String server_ip;
	protected String server_port;
	protected String file_path;

	// uri for connection
	protected final URI uri;

	// file members
	protected FileWriter fstream;
	protected BufferedWriter out;

	// logger
	private final static Log LOG = LogFactory.getLog(DataPathTestClient.class.getCanonicalName());

	protected DataPathTest(String args[]) {
		this.parseCommandArgs(args);
		uri = generateUri();
		if (uri == null) {
			System.out.println("Bad URI, Aborting test...");
			System.exit(0);
		}
	}

	// build URI from command line parameters
	protected URI generateUri() {
		String url_string = "rdma://" + server_ip + ":" + server_port;
		try {
			return new URI(url_string);
		} catch (URISyntaxException e) {
			System.out.println("Bad URI given\n");
			LOG.error("Bad URI given");
			e.printStackTrace();
		}
		return null;
	}

	public void stopTest() {
		LOG.debug("finishing test");
	}

	// parse command line parameters into class members
	protected void parseCommandArgs(String[] args) {
		server_ip = args[0];
		server_port = args[1];
		num_of_threads = Integer.parseInt(args[2]);
		inMsg_size = Integer.parseInt(args[3]);
		outMsg_size = Integer.parseInt(args[4]);
		burst_size = Integer.parseInt(args[5]);
	}
}
