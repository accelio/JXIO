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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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
	private Chapter          chapter;
	private List<String>     characterTypes;
	private List<Character>  machines;
	private List<Character>  processes;
	private List<Character>  servers;
	private List<Character>  clients;

	/**
     * A chapter from the story, segement of the story macthing a mahcine.
     */
	private class Chapter {
		public String                                   myIP;
		public Character                                myMachine;
		public List<Character>                          myProcesses;
		public Map<Character, List<Character>>          processServers;
		public Map<Character, List<Character>>          processClients;
		public Map<Character, List<ServerPortalPlayer>> processServerPlayers;
		public Map<Character, List<ClientPlayer>>       processClientPlayers;
		public Map<Character, Integer>                  processMaxDuration;
		public Map<Character, WorkerThreads>            processWorkerThreads;
	}

	/**
     * Constructs an new StoryRunner
     */
	public JXIOStoryRunner() {
		this.story = new Story();
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
			System.out.println("=============");
			System.out.println("Story Summary");
			System.out.println("=============");
			printSummary();

		} catch (ParserConfigurationException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (SAXException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (IOException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		}
	}

	/**
     * Runs the story.
     */
	public void run() {

		System.out.println("=============");
		System.out.println("Story Running");
		System.out.println("=============");

		// Get my chapter in the story
		chapter = readChapter();
		if (chapter == null) {
			System.out.println("[ERROR] Failed to read chapter.");
			return;
		}

		// Run
		// Create Tasks
		ForkJoinPool f = new ForkJoinPool();
		List<JXIOProcessTask> tasks = new ArrayList<JXIOProcessTask>();
		for (Character process : chapter.myProcesses) {
			tasks.add(new JXIOProcessTask(chapter.myMachine, process, chapter.processServerPlayers.get(process),
			        chapter.processClientPlayers.get(process), chapter.processWorkerThreads.get(process),
			        chapter.processMaxDuration.get(process)));
		}
		// Run Tasks
		f.invokeAll(tasks);
	}

	private Chapter readChapter() {

		Chapter myChapter = new Chapter();

		// Get my IP
		myChapter.myIP = getIP();
		if (myChapter.myIP == null) {
			System.out.println("[ERROR] No Infiniband found on this machine!");
			return null;
		}
		System.out.println("Your machine's IP is " + myChapter.myIP);

		// Fetch my machine
		myChapter.myMachine = getCharacterFromListByAttribute(machines, "address", myChapter.myIP);
		if (myChapter.myMachine == null) {
			System.out.println("[ERROR] No machine with your IP found in the story!");
			return null;
		}
		System.out.println("Your name is " + myChapter.myMachine.getAttribute("name"));

		// Fetch my processes
		myChapter.myProcesses = getCharactersFromListByAttribute(processes, "machine", myChapter.myMachine
		        .getAttribute("id"));
		if (myChapter.myProcesses.size() == 0) {
			System.out.println("No process found for your machine!");
			return null;
		}

		// Map
		// Map servers To Processes
		myChapter.processServers = mapCharactersByCharacter(servers, "process", processes);
		// Map clients To Processes
		myChapter.processClients = mapCharactersByCharacter(clients, "process", processes);
		// Map ServersPlayers, ClientsPlayers, Max Duration Per Process and Worker Theards
		int errorCheck = mapPlayers(myChapter);
		if (errorCheck == -1) {
			// An error occurred. Story should not be run.
			return null;
		}
		return myChapter;
	}

	/**
     * Retrive the current machine's Infiniband IPv4 address
     * 
     * @return An IPv4 address
     */
	private String getIP() {
		String myIP = null;
		try {
			Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()) {
				NetworkInterface ni = nis.nextElement();
				if (ni.getDisplayName().contains("ib")) {
					for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
						if (ia.getNetworkPrefixLength() == 8 || ia.getNetworkPrefixLength() == 16
						        || ia.getNetworkPrefixLength() == 24) {
							myIP = ia.getAddress().getHostAddress();
						}
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return myIP;
	}

	/**
     * Retrieve all characters matching a specific type.
     * 
     * @param charcterType
     *            A character type in single form.
     * @return A list of all characters of the requested type.
     */
	private List<Character> getCharacters(String charcterType) {
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
     * Returns a mapping a characters to a list of matching characters by a given attribute.
     * 
     * @param supportingCharacter
     *            A list of characters the sublists of it will be the values of the returned map.
     * @param attribute
     *            The attribute by which the supporting characters will be matched to the main character.
     * @param Characters
     *            A list of characters the will be the keys of the returned map.
     * @return A map that matches the characters to the relevant supporting characters.
     */
	private Map<Character, List<Character>> mapCharactersByCharacter(List<Character> supportingCharacter,
	        String attribute, List<Character> Characters) {
		Map<Character, List<Character>> map = new HashMap<Character, List<Character>>();
		for (Character character : Characters) {
			String id = character.getAttribute("id");
			List<Character> list = getCharactersFromListByAttribute(supportingCharacter, attribute, id);
			map.put(character, list);
		}
		return map;
	}

	/**
     * Retrieves a specific matching character from a list of characters.
     * 
     * @param characters
     *            The list of characters.
     * @param attribute
     *            The atrribute to match.
     * @param value
     *            The value of the requsted attribute to match.
     * @return The charcater from the list with there attribute matching the given value.
     */
	private Character getCharacterFromListByAttribute(List<Character> characters, String attribute, String value) {
		for (Character character : characters) {
			if (character.getAttribute(attribute).equals(value)) {
				return character;
			}
		}
		return null;
	}

	/**
     * Retrieves a list of matching characters from a list of characters.
     * 
     * @param characters
     *            The list of characters.
     * @param attribute
     *            The atrribute to match.
     * @param value
     *            The value of the requsted attribute to match.
     * @return A list of charcater from the list with there attribute matching the given value.
     */
	private List<Character> getCharactersFromListByAttribute(List<Character> characters, String attribute, String value) {
		List<Character> list = new ArrayList<Character>();
		for (Character character : characters) {
			if (character.getAttribute(attribute).equals(value)) {
				list.add(character);
			}
		}
		return list;
	}

	/**
     * Updates the maps of a given chapter to map and store server players, client players, max durations and worker
     * threads.
     * 
     * @param chapter
     *            The chapter which it's maps should be updated.
     * @return 0 on success, -1 otherwise.
     */
	private int mapPlayers(Chapter chapter) {

		chapter.processServerPlayers = new HashMap<Character, List<ServerPortalPlayer>>();
		chapter.processClientPlayers = new HashMap<Character, List<ClientPlayer>>();
		chapter.processMaxDuration = new HashMap<Character, Integer>();
		chapter.processWorkerThreads = new HashMap<Character, WorkerThreads>();

		for (Character myProcess : chapter.myProcesses) {

			// Configure Worker Theardes
			int numWorkerThreads = Integer.valueOf(myProcess.getAttribute("num_eqhs"));
			WorkerThreads myWorkers = new WorkerThreads(numWorkerThreads, chapter.processServers.get(myProcess).size());
			chapter.processWorkerThreads.put(myProcess, myWorkers);

			// Configure Servers
			int serversMaxDuration = configureServers(chapter, myProcess);

			// Configure Clients
			int clientsMaxDuration = configureClients(chapter, myProcess);

			// Check for errors
			if (serversMaxDuration < 0 || clientsMaxDuration < 0) {
				// An error occurred
				return -1;
			}

			// Set process' Max Duration
			int maxDuration = (serversMaxDuration > clientsMaxDuration) ? serversMaxDuration : clientsMaxDuration;
			chapter.processMaxDuration.put(myProcess, maxDuration);
		}
		return 0;
	}

	/**
     * Configures the client players of the given process.
     * 
     * @param myProcess
     *            A character representing a process.
     * @return The maximal duration of a client in the given process.
     */
	private int configureClients(Chapter chapter, Character myProcess) {
		int maxDuration = 0;
		// Configure Clients
		List<Character> myClients = chapter.processClients.get(myProcess);
		List<ClientPlayer> clientPlayers = new ArrayList<ClientPlayer>();
		for (Character client : myClients) {
			try {
				// Get client parameters
				int id = Integer.valueOf(client.getAttribute("id"));
				Character process = getCharacterFromListByAttribute(processes, "id", client.getAttribute("process"));
				int duration = Integer.valueOf(client.getAttribute("duration"));
				int batch = Integer.valueOf(client.getAttribute("batch"));
				Character server = getCharacterFromListByAttribute(servers, "id", client.getAttribute("server"));
				int startDelay = Integer.valueOf(client.getAttribute("start_delay"));
				int tps = Integer.valueOf(client.getAttribute("tps"));

				// Get client hoops
				List<Character> supportingCharacters = client.getSupportingCharacters();
				List<Character> hoops = null;
				for (Character charcter : supportingCharacters) {
					if (charcter.getCharacterType().equals("hoops")) {
						hoops = charcter.getSupportingCharacters();
						break;
					}
				}

				// Get client msgs
				int count = Integer.valueOf(client.getAttribute("msg_count_factor"));
				int in = Integer.valueOf(client.getAttribute("msg_size_in_factor"));
				int out = Integer.valueOf(client.getAttribute("msg_size_out_factor"));
				MsgPoolData pool = new MsgPoolData(count, in, out);

				// Resolve hostname and port
				Character serverProcess = getCharacterFromListByAttribute(processes, "id", server
				        .getAttribute("process"));
				Character machine = getCharacterFromListByAttribute(machines, "id", serverProcess
				        .getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				int port = Integer.valueOf(server.getAttribute("port"));
				int reject = Integer.valueOf(client.getAttribute("reject"));
				String suffix = "";
				if (reject == 1) {
					suffix = "toReject";
				}
				URI uri = new URI("rdma://" + hostname + ":" + port + "/" + suffix);

				// Update max duration
				if (startDelay + duration > maxDuration) {
					maxDuration = startDelay + duration;
				}

				// Add client
				ClientPlayer cp = new ClientPlayer(id, uri, startDelay, duration, tps, pool);
				clientPlayers.add(cp);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		// Set process' ClientPlayers
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
	private int configureServers(Chapter chapter, Character myProcess) {
		int maxDuration = 0;
		// Configure Servers
		List<Character> myServers = chapter.processServers.get(myProcess);
		List<ServerPortalPlayer> serverPlayers = new ArrayList<ServerPortalPlayer>();
		for (Character server : myServers) {
			try {
				// Get server parameters
				int id = Integer.valueOf(server.getAttribute("id"));
				int port = Integer.valueOf(server.getAttribute("port"));
				Character process = getCharacterFromListByAttribute(processes, "id", server.getAttribute("process"));
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
					return -1;
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
				Character machine = getCharacterFromListByAttribute(machines, "id", process.getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				URI uri = new URI("rdma://" + hostname + ":" + port + "/");

				// Update max duration
				if (startDelay + duration > maxDuration) {
					maxDuration = startDelay + duration;
				}

				// Add server
				ServerPortalPlayer sp = new ServerPortalPlayer(numWorkers, id, 0, uri, startDelay, duration,
				        chapter.processWorkerThreads.get(myProcess), msgPools);
				serverPlayers.add(sp);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		// Set process' ServerPlayers
		chapter.processServerPlayers.put(myProcess, serverPlayers);
		return maxDuration;
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

	/**
     * A JXIO Process to be run as a forked task.
     */
	static public class JXIOProcessTask extends RecursiveTask<Integer> implements Callable<Integer> {

		private final Character                machine;
		private final Character                process;
		private final List<ServerPortalPlayer> serverPlayers;
		private final List<ClientPlayer>       clientPlayers;
		private final WorkerThreads            workers;
		private final int                      maxDuration;

		/**
         * Constructs a new JXIOProcessTask
         * 
         * @param machine
         *            The character representing to machine on which to process runs.
         * @param process
         *            The character representing the JXIO process.
         * @param serverPlayers
         *            A list of ServerPortalPlayers that should run on this process.
         * @param clientPlayers
         *            A list of ClientPlayers that should run on this process.
         * @param workers
         *            The WorkrTheards of the process.
         * @param maxDuration
         *            The maximal time duration for the process to run.
         */
		public JXIOProcessTask(Character machine, Character process, List<ServerPortalPlayer> serverPlayers,
		        List<ClientPlayer> clientPlayers, WorkerThreads workers, int maxDuration) {
			this.machine = machine;
			this.process = process;
			this.serverPlayers = serverPlayers;
			this.clientPlayers = clientPlayers;
			this.workers = workers;
			this.maxDuration = maxDuration;
		}

		/**
         * The funciton that is invoked when running the forked process. Computes the total running time of the process
         * (can be used for benchmarking).
         */
		@Override
		protected Integer compute() {

			// Set strting time
			int startTime = 0;

			// Run
			System.out.println("=> Process " + process.getAttribute("id") + ": " + serverPlayers.size() + " servers "
			        + clientPlayers.size() + " clients\n");
			int numWorkerThreads = Integer.valueOf(this.process.getAttribute("num_eqhs"));
			LOG.info("There are " + numWorkerThreads + " working threads");

			// Run Server Players
			for (ServerPortalPlayer sp : this.serverPlayers) {
				workers.getWorkerThread().addWorkAction(sp.getAttachAction());
			}
			// Run Client Players
			for (ClientPlayer cp : this.clientPlayers) {
				workers.getWorkerThread().addWorkAction(cp.getAttachAction());
			}

			// Sleeping for max_duration + 2 extra sec
			LOG.info("max duration of all threads is " + maxDuration + " seconds");
			try {

				Thread.sleep((maxDuration + 2) * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println("=====================================");
			System.out.println("done sleeping! must kill all threads!");
			System.out.println("=====================================");

			workers.close();

			System.out.println("=============");
			System.out.println("done killing!");
			System.out.println("=============");

			// Set ending time
			int endTime = 0;

			// return running time
			int runningTime = endTime - startTime;
			return runningTime;
		}

		/**
         * The funciton that is invoked when running a collection of forked processes. Computes the total running time
         * of the process (can be used for benchmarking).
         */
		public Integer call() throws Exception {
			return compute();
		}
	}

}
