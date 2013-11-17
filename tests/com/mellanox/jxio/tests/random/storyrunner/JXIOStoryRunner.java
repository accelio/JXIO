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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JXIOStoryRunner implements StoryRunner {

	private Document      docRead;
	private List<Machine> machines;
	private List<Process> processes;
	private List<Client>  clients;
	private List<Server>  servers;
	private WorkerThreads workers;

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

			// Get Machines
			machines = getMachines();
			// Get Processes
			processes = getProcesses();
			// Get Clients
			clients = getClients();
			// Get Servers
			servers = getServers();

			// Print Summary
			System.out.println("Finised reading the story.");
			System.out.println("Story Summary:");
			printSummary();

		} catch (ParserConfigurationException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (SAXException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (IOException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		}
	}

	private List<Server> getServers() {
		List<Server> servers = new ArrayList<Server>();
		// Read given tag
		NodeList nodeList = docRead.getElementsByTagName("server");
		// Iterate over all occurrences
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				// Create a new server
				String id = getTagValue(element, "id");
				String process = getTagValue(element, "process");
				String port = getTagValue(element, "port");
				String duration = getTagValue(element, "duration");
				String maxWorkers = getTagValue(element, "max_workers");
				String delay = getTagValue(element, "delay");
				String startDelay = getTagValue(element, "start_delay");
				String tps = getTagValue(element, "tps");
				// TODO Add msg_pools

				Server server;
				if (id != null) {
					server = new Server(Integer.valueOf(id), Integer.valueOf(process), Integer.valueOf(port), Integer
					        .valueOf(duration), Integer.valueOf(maxWorkers), Integer.valueOf(delay), Integer
					        .valueOf(startDelay), Integer.valueOf(tps));
					// Add process
					servers.add(server);
				}
			}
		}
		return servers;
	}

	private List<Client> getClients() {
		List<Client> clients = new ArrayList<Client>();
		// Read given tag
		NodeList nodeList = docRead.getElementsByTagName("client");
		// Iterate over all occurrences
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				// Create a new client
				String id = getTagValue(element, "id");
				String process = getTagValue(element, "process");
				String server = getTagValue(element, "server");
				String duration = getTagValue(element, "duration");
				String batch = getTagValue(element, "batch");
				String startDelay = getTagValue(element, "start_delay");
				String tps = getTagValue(element, "tps");

				Client client;
				if (id != null) {
					client = new Client(Integer.valueOf(id), Integer.valueOf(process), Integer.valueOf(server), Integer
					        .valueOf(duration), Integer.valueOf(batch), Integer.valueOf(startDelay), Integer
					        .valueOf(tps));
					// Add process
					clients.add(client);
				}
			}
		}
		return clients;
	}

	private List<Process> getProcesses() {
		List<Process> processes = new ArrayList<Process>();
		// Read given tag
		NodeList nodeList = docRead.getElementsByTagName("process");
		// Iterate over all occurrences
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				// Create a new process
				String id = getTagValue(element, "id");
				String location = getTagValue(element, "location");
				String numEqhs = getTagValue(element, "num_eqhs");
				String timeout = getTagValue(element, "timeout");
				Process process;
				if (id != null) {
					process = new Process(Integer.valueOf(id), location, Integer.valueOf(numEqhs), Integer
					        .valueOf(timeout));
					// Add process
					processes.add(process);
				}
			}
		}
		return processes;
	}

	private List<Machine> getMachines() {
		List<Machine> machines = new ArrayList<Machine>();
		// Read given tag
		NodeList nodeList = docRead.getElementsByTagName("machine");
		// Iterate over all occurrences
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				// Create a new machine
				String id = getTagValue(element, "id");
				String manageInterface = getTagValue(element, "manag_interface");
				String address = getTagValue(element, "address");
				String name = getTagValue(element, "name");
				String type = getTagValue(element, "type");
				Machine machine;
				if (id != null) {
					machine = new Machine(Integer.valueOf(id), manageInterface, address, name, type);
					// Add machine
					machines.add(machine);
				}
			}
		}
		return machines;
	}

	private String getTagValue(Element element, String tag) {
		String value = null;
		// Iterate over all sub-tags
		for (int j = 1; j < element.getChildNodes().getLength(); j++) {
			Node item = element.getChildNodes().item(j);
			// Check for data tags
			if (!item.getNodeName().equals("#text")) {
				// Get attribute's name
				String att = item.getNodeName();
				if (att.equals(tag)) {
					// Get attribute's value
					Node valueNode = item.getAttributes().getNamedItem("value");

					// If no value found, as it is a title tag, recurse into it's sub-tags
					if (valueNode != null) {
						value = valueNode.getNodeValue();
						return value;
					}
					// else {
					// // Create a supporting character
					// Character supportingCharacter = new Character();
					// supportingCharacter.setCharacterType(att);
					// // Update it's attributes
					// updateAttributes((Element) item, supportingCharacter);
					// // Save to main character
					// mainCharacter.addSupportingCharacter(supportingCharacter);
					// }
				}
			}
		}
		return value;
	}

	/**
     * Runs the story.
     */

	public void run() {
		System.out.println("Story Running");
		System.out.println("=============");
		printSummary();

		// Simple Client-Server:
		// ServerManagerPlayer sm = new ServerManagerPlayer(new URI("rdma://0:52002/"), 0, 12,
		// storyRunner.getWorkerThreads());
		// storyRunner.getWorkerThreads().getWorkerThread().addWorkAction(sm.getAttachAction());

		// Thread.sleep(10);

		ClientPlayer c1 = null;
		try {
			c1 = new ClientPlayer(new URI("rdma://0:52002/"), 0, 6, 2);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getWorkerThreads().getWorkerThread().addWorkAction(c1.getAttachAction());

		// ClientPlayer c2 = new ClientPlayer(new URI("rdma://0:52002/"), 4, 6, 3);
		// storyRunner.getWorkerThreads().getWorkerThread().addWorkAction(c2.getAttachAction());
	}

	private void printSummary() {
		System.out.print("Machines:");
		for (Machine machine : machines) {
			System.out.print(" " + machine.getName());
		}
		System.out.println();
		System.out.print("Processes:");
		for (Process process : processes) {
			System.out.print(" " + process.getId());
		}
		System.out.println();
		System.out.print("Clients:");
		for (Client client : clients) {
			System.out.print(" " + client.getId());
		}
		System.out.println();
		System.out.print("Servers:");
		for (Server server : servers) {
			System.out.print(" " + server.getId());
		}
		System.out.println();
	}

}
