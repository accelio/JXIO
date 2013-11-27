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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.mellanox.jxio.tests.random.Character;
import com.mellanox.jxio.tests.random.Story;

public class JXIOStoryRunner implements StoryRunner {

	private final static Log LOG = LogFactory.getLog(JXIOStoryRunner.class.getSimpleName());

	private Document         docRead;
	private Story            story;
	private List<String>     characterTypes;
	private List<Character>  machines;
	private List<Character>  processes;
	private List<Character>  servers;
	private List<Character>  clients;
	private WorkerThreads    workers;

	/**
	 * Constructs an new StoryRunner
	 * 
	 */
	public JXIOStoryRunner() {
		this.story = new Story();
	}

	/**
	 * Retrieves all worker threads
	 * 
	 * @return The worker threads of the story runner.
	 */
	public WorkerThreads getWorkerThreads() {
		return this.workers;
	}

	/**
	 * Reads the story XML file.
	 */
	public void read(File storyFile) {
		try {
			// Set needed XML document handling objects
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			docRead = dBuilder.parse(storyFile);
			docRead.getDocumentElement().normalize();

			// Get characters
			characterTypes = Story.getStoryCharacters(docRead);
			// Remove the "story" character
			characterTypes.remove(0);
			// Read all characters
			for (String characterType : characterTypes) {
				// String singleCharacter = "machine";//Story.singleFromPlural(docRead, characterType);
				Character character = Story.readCharacter(docRead, characterType);
				story.addCharacter(character);
			}

			// Get Machines
			machines = getCharacters("machines");
			// Get Processes
			processes = getCharacters("processes");
			// Get Clients
			clients = getCharacters("clients");
			// Get Servers
			servers = getCharacters("servers");

			// Print Summary
			System.out.println("Finised reading the story.");

		} catch (ParserConfigurationException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (SAXException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (IOException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		}
	}

	/**
	 * Retrieve all characters matching a specific type.
	 * 
	 * @param charcterType
	 *            A character type in single form.
	 * @return A list of all characters of the requested type.
	 */
	public List<Character> getCharacters(String charcterType) {
		List<Character> chracterList = new ArrayList<Character>();
		for (Character character : story.getCharacters()) {
			if (character.getCharacterType().equals(charcterType)) {
				chracterList = character.getSupportingCharacters();
				return chracterList;
			}
		}
		return chracterList;
	}

	/**
	 * Runs the story.
	 */
	public void run() {
		System.out.println("=============");
		System.out.println("Story Summary");
		System.out.println("=============");
		printSummary();
		System.out.println("=============");
		System.out.println("Story Running");
		System.out.println("=============");

		Character p = processes.get(0);// for now, there is only one process
		int numWorkerThreads = Integer.valueOf(p.getAttribute("num_eqhs"));

		LOG.info("there are " + numWorkerThreads + " working threads");
		// create worker threads
		this.workers = new WorkerThreads(numWorkerThreads, servers.size());

		// Simple Client-Server

		// this variable will hold the max duration of client/server in this process
		int max_duration = 0;

		// Configure Servers
		int i = 0;
		ServerPortalPlayer[] serverPlayers = new ServerPortalPlayer[servers.size()];
		for (Character server : servers) {
			try {
				// Get server parameters
				int id = Integer.valueOf(server.getAttribute("id"));
				int port = Integer.valueOf(server.getAttribute("port"));
				Character process = getCharacterFromListById(processes, server.getAttribute("process"));
				int duration = Integer.valueOf(server.getAttribute("duration"));
				int numWorkers = Integer.valueOf(server.getAttribute("num_workers"));
				int delay = Integer.valueOf(server.getAttribute("delay"));
				int startDelay = Integer.valueOf(server.getAttribute("start_delay"));
				int tps = Integer.valueOf(server.getAttribute("tps"));

				// Get server msg pools
				List<Character> supportingCharacters = server.getSupportingCharacters();
				List<Character> msgPoolsList = null;
				for (Character charcter : supportingCharacters) {
					if (charcter.getCharacterType().equals("msg_pools")) {
						msgPoolsList = charcter.getSupportingCharacters();
					}
				}
				if (msgPoolsList == null) {
					System.out.println("[ERROR] No msg_pool defined for server " + server.getAttribute("id") + "!");
					return;
				}
				ArrayList<MsgPoolData> msgPools = new ArrayList<MsgPoolData>();
				for (Character msgPool : msgPoolsList) {
					int count = Integer.valueOf(msgPool.getAttribute("msg_pool_count"));
					int in = Integer.valueOf(msgPool.getAttribute("msg_pool_size_in"));
					int out = Integer.valueOf(msgPool.getAttribute("msg_pool_size_out"));
					MsgPoolData pool = new MsgPoolData(count, in, out);
					msgPools.add(pool);
				}

				// Resolve hostname
				Character machine = getCharacterFromListById(machines, process.getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				URI uri = new URI("rdma://" + hostname + ":" + port + "/");
				if (startDelay + duration > max_duration) {
					max_duration = startDelay + duration;
				}

				ServerPortalPlayer sp = new ServerPortalPlayer(numWorkers, id, 0, uri, startDelay, duration,
				        getWorkerThreads(), msgPools);
				serverPlayers[i] = sp;
				i++;
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		// Configure Clients
		ClientPlayer[] clientPlayers = new ClientPlayer[clients.size()];
		i = 0;
		for (Character client : clients) {
			try {
				// Get client parameters
				int id = Integer.valueOf(client.getAttribute("id"));
				int process = Integer.valueOf(client.getAttribute("process"));
				int duration = Integer.valueOf(client.getAttribute("duration"));
				int batch = Integer.valueOf(client.getAttribute("batch"));
				Character server = getCharacterFromListById(servers, client.getAttribute("server"));
				int startDelay = Integer.valueOf(client.getAttribute("start_delay"));
				int tps = Integer.valueOf(client.getAttribute("tps"));

				// Resolve hostname and port
				Character server_process = getCharacterFromListById(processes, server.getAttribute("process"));
				Character machine = getCharacterFromListById(machines, server_process.getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				int port = Integer.valueOf(server.getAttribute("port"));
				URI uri = new URI("rdma://" + hostname + ":" + port + "/");

				if (startDelay + duration > max_duration) {
					max_duration = startDelay + duration;
				}

				// Get client msgs
				int count = Integer.valueOf(client.getAttribute("msg_count_factor"));
				int in = Integer.valueOf(client.getAttribute("msg_size_in_factor"));
				int out = Integer.valueOf(client.getAttribute("msg_size_out_factor"));

				ClientPlayer cp = new ClientPlayer(id, uri, startDelay, duration, tps, new MsgPoolData(count, in, out));
				clientPlayers[i] = cp;
				i++;
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		// Run Server Players
		for (ServerPortalPlayer spp : serverPlayers) {
			getWorkerThreads().getWorkerThread().addWorkAction(spp.getAttachAction());
		}
		// Run Client Players
		for (ClientPlayer cp : clientPlayers) {
			getWorkerThreads().getWorkerThread().addWorkAction(cp.getAttachAction());
		}

		LOG.info("max duration of all threads is " + max_duration + " seconds");
		try {
			// sleeping for max_duration + 2 extra sec
			Thread.sleep((max_duration + 2) * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("=============");
		System.out.println("done sleeping! must kill all threads!");
		System.out.println("=============");

		getWorkerThreads().close();

		System.out.println("=============");
		System.out.println("done killing!");
		System.out.println("=============");
	}

	/**
	 * Retrieves a specific character from a list of characters.
	 * 
	 * @param characters
	 *            The list of characters.
	 * @param id
	 *            The ID of the requsted character.
	 * @return The charcater from the list with there ID matching the given ID.
	 */
	private Character getCharacterFromListById(List<Character> characters, String id) {
		for (Character character : characters) {
			if (character.getAttribute("id").equals(id)) {
				return character;
			}
		}
		return null;
	}

	/**
	 * Prints a summary of the story.
	 */
	private void printSummary() {
		System.out.print("Machines:");
		for (Character machine : machines) {
			System.out.print(" " + machine.getAttribute("id"));
		}
		System.out.println();
		System.out.print("Processes:");
		for (Character process : processes) {
			System.out.print(" " + process.getAttribute("id"));
		}
		System.out.println();
		System.out.print("Clients:");
		for (Character client : clients) {
			System.out.print(" " + client.getAttribute("id"));
		}
		System.out.println();
		System.out.print("Servers:");
		for (Character server : servers) {
			System.out.print(" " + server.getAttribute("id"));
		}
		System.out.println();
	}

}
