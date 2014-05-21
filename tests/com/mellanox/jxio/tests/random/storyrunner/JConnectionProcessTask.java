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
package com.mellanox.jxio.tests.random.storyrunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.mellanox.jxio.jxioConnection.JxioConnection;
import com.mellanox.jxio.tests.random.Character;
import com.mellanox.jxio.tests.random.Story;

/**
 * A JXIO Process to be run as a forked task.
 */
public class JConnectionProcessTask extends JXIOProcessTask {

	/**
	 * The main JXIO Process Task that runs.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (!argsCheck(args)) {
			return;
		} else {
			try {
				System.out.println("\nRunning process " + args[1] + "!");
				new JConnectionProcessTask(args[0], args[1]).call();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Constructs a new JXIOProcessTask
	 */
	public JConnectionProcessTask(String storyFile, String processID) {
		super(storyFile, processID);
	}

	/**
	 * Configures the client players of the given process.
	 * 
	 * @param myProcess
	 *            A character representing a process.
	 * @return The maximal duration of a client in the given process.
	 */
	@Override
	protected int configureClients(Chapter chapter, Character myProcess) {
		int maxDuration = 0;
		// Configure Clients
		List<Character> myClients = chapter.processClients.get(myProcess);
		List<GeneralPlayer> clientPlayers = new ArrayList<GeneralPlayer>();
		for (Character client : myClients) {
			int counter = 1;
			try {
				String uriQueryStr = "";
				// Get client parameters
				int id = Integer.valueOf(client.getAttribute("id"));
				int duration = Integer.valueOf(client.getAttribute("duration"));
				Character server = Character.getCharacterFromListByAttribute(servers, "id",
				        client.getAttribute("server"));
				int startDelay = Integer.valueOf(client.getAttribute("start_delay"));

				int repeats = Integer.valueOf(client.getAttribute("repeats"));
				int repeatDelay = Integer.valueOf(client.getAttribute("repeat_delay"));
				int violentExit = Integer.valueOf(client.getAttribute("violent_exit"));
				long msgPoolMem = Long.valueOf(server.getAttribute("msg_pool_mem"));
				int rate = Integer.valueOf(client.getAttribute("rate"));
				long size = Long.valueOf(client.getAttribute("size"));

				// Resolve hostname and port
				Character serverProcess = Character.getCharacterFromListByAttribute(processes, "id",
				        server.getAttribute("process"));
				Character machine = Character.getCharacterFromListByAttribute(machines, "id",
				        serverProcess.getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				int port = Integer.valueOf(server.getAttribute("port"));
				int reject = Integer.valueOf(client.getAttribute("reject"));

				uriQueryStr += "?size=" + size;
				if (reject == 1) {
					uriQueryStr += "reject=1";
				}
				// Create URI
				URI uri = new URI("rdma://" + hostname + ":" + port + "/" + uriQueryStr);
				// Update max duration
				if (startDelay + duration > maxDuration) {
					maxDuration = startDelay + duration;
				}
				int clientStartDelay;
				for (int i = 0; i < repeats + 1; i++) {
					clientStartDelay = (duration + repeatDelay) * i + startDelay;
					JConnectionClientPlayer cp = new JConnectionClientPlayer(id, uri, clientStartDelay, duration, msgPoolMem,
					        rate, violentExit, seed + counter * 17, size);
					clientPlayers.add(cp);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			counter++;
		}
		chapter.processClientPlayers.put(myProcess, clientPlayers);
		return maxDuration;
	}

	/**
	 * Configures the server players of the given process.
	 * 
	 * @param myProcess
	 *            A character representing a process.
	 * @return The maximal duration of a server in the given process, or -1 on error.
	 */
	@Override
	protected int configureServers(Chapter chapter, Character myProcess) {
		int maxDuration = 0;
		// Configure Servers
		List<Character> myServers = chapter.processServers.get(myProcess);
		List<GeneralPlayer> serverPlayers = new ArrayList<GeneralPlayer>();
		for (Character server : myServers) {
			int counter = 1;
			try {
				// Get server parameters
				int id = Integer.valueOf(server.getAttribute("id"));
				int port = Integer.valueOf(server.getAttribute("port"));
				Character process = Character.getCharacterFromListByAttribute(processes, "id",
				        server.getAttribute("process"));
				int duration = Integer.valueOf(server.getAttribute("duration"));
				int numWorkers = Integer.valueOf(server.getAttribute("num_workers"));
				int rate = Integer.valueOf(server.getAttribute("rate"));
				int startDelay = Integer.valueOf(server.getAttribute("start_delay"));
				int violentExit = Integer.valueOf(server.getAttribute("violent_exit"));
				long msgPoolMem = Long.valueOf(server.getAttribute("msg_pool_mem"));
				// Resolve hostname
				Character machine = Character.getCharacterFromListByAttribute(machines, "id",
				        process.getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				URI uri = new URI("rdma://" + hostname + ":" + port + "/");
				// Update max duration
				if (startDelay + duration > maxDuration) {
					maxDuration = startDelay + duration;
				}
				// Add server
				JConnectionServerPlayer sp = new JConnectionServerPlayer(numWorkers, id, 0, uri, startDelay, duration,
				        chapter.processWorkerThreads.get(myProcess), msgPoolMem, violentExit, seed + counter * 3, rate);
				serverPlayers.add(sp);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			counter++;
		}
		// Set process' ServerPlayers
		chapter.processServerPlayers.put(myProcess, serverPlayers);
		return maxDuration;
	}
}