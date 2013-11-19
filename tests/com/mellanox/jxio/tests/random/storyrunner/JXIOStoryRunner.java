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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.mellanox.jxio.tests.random.Character;
import com.mellanox.jxio.tests.random.Story;

public class JXIOStoryRunner implements StoryRunner {

	private Document        docRead;
	private Story           story;
	private List<String>    characterTypes;
	private List<Character> machines;
	private List<Character> processes;
	private List<Character> servers;
	private List<Character> clients;
	private WorkerThreads   workers;

	/**
     * Constructs an new StoryRunner with infinite worker threads.
     * 
     * @param xmlFile
     *            The story XML file.
     */
	public JXIOStoryRunner() {
		this(-1);
	}

	/**
     * Constructs an new StoryRunner.
     * 
     * @param xmlFile
     *            The story XML file.
     * @param workerThreads
     *            Number of worker threads needed.
     */
	public JXIOStoryRunner(int workerThreads) {
		this.story = new Story();
		this.workers = new WorkerThreads(workerThreads);
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
	 * @param charcterType A character type in single form.
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

		// Simple Client-Server
		// Configure Servers
		int i = 0;
		ServerPortalPlayer[] serverPlayers = new ServerPortalPlayer[servers.size()];
		for (Character server : servers) {
			try {
				String hostname = server.getAttribute("location");
				int port = Integer.valueOf(server.getAttribute("port"));
				int startDelay = Integer.valueOf(server.getAttribute("start_delay"));
				int duration = Integer.valueOf(server.getAttribute("duration"));

				ServerPortalPlayer sp = new ServerPortalPlayer(new URI("rdma://" + hostname + ":" + port + "/"),
				        startDelay, duration, getWorkerThreads());
				serverPlayers[i] = sp;
				i++;
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		// Thread.sleep(10);

		// Configure Clients
		ClientPlayer[] clientPlayers = new ClientPlayer[clients.size()];
		i = 0;
		for (Character client : clients) {
			try {
				String hostname = "0";
				// String hostname = server.getProcess().getLocation().getHostname();
				int port = 500;
				int startDelay = Integer.valueOf(client.getAttribute("start_delay"));
				int duration = Integer.valueOf(client.getAttribute("duration"));
				int tps = Integer.valueOf(client.getAttribute("tps"));
				ClientPlayer cp = new ClientPlayer(new URI("rdma://" + hostname + ":" + port + "/"), startDelay,
				        duration, tps);
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
