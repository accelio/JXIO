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
		
		
		Character p = processes.get(0);//for now, there is only one process
		int numWorkerThreads = Integer.valueOf(p.getAttribute("num_eqhs"));
		
		System.out.println("there are " + numWorkerThreads + " working threads");
		//create worker threads
		this.workers = new WorkerThreads(numWorkerThreads);

		// Simple Client-Server
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
				int maxWorkers = Integer.valueOf(server.getAttribute("max_workers"));
				int delay = Integer.valueOf(server.getAttribute("delay"));
				int startDelay = Integer.valueOf(server.getAttribute("start_delay"));
				int tps = Integer.valueOf(server.getAttribute("tps"));
				
				// Resolve hostname
				Character machine = getCharacterFromListById(machines, process.getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				
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
				// Get client parameters
				int id = Integer.valueOf(client.getAttribute("id"));
				int process = Integer.valueOf(client.getAttribute("process"));
				int duration = Integer.valueOf(client.getAttribute("duration"));
				int batch = Integer.valueOf(client.getAttribute("batch"));
				Character server = getCharacterFromListById(servers, client.getAttribute("server"));
				int startDelay = Integer.valueOf(client.getAttribute("start_delay"));
				int tps = Integer.valueOf(client.getAttribute("tps"));
				
				// Resolve hostname and port
				Character server_process =  getCharacterFromListById(processes, server.getAttribute("process"));
				Character machine = getCharacterFromListById(machines, server_process.getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				int port = Integer.valueOf(server.getAttribute("port"));
				
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
	 * Retrieves a specific character from a list of characters.
	 * @param characters The list of characters.
	 * @param id The ID of the requsted character.
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
