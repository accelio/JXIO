package com.mellanox.jxio.tests.random.storyrunner;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.mellanox.jxio.tests.random.Character;
import com.mellanox.jxio.tests.random.Story;

/**
 * A JXIO Process to be run as a forked task.
 */
public class JXIOProcessTask implements Callable<Integer> {

	private final static Log         LOG = LogFactory.getLog(JXIOProcessTask.class.getSimpleName());

	private final String             storyFile;
	private final String             processID;
	private Document                 docRead;
	private Story                    story;
	private long                     seed;
	private Chapter                  chapter;
	private List<String>             characterTypes;
	private List<Character>          machines;
	private List<Character>          processes;
	private List<Character>          servers;
	private List<Character>          clients;

	private Character                machine;
	private Character                process;
	private List<ServerPortalPlayer> serverPlayers;
	private List<ClientPlayer>       clientPlayers;
	private WorkerThreads            workers;
	private int                      maxDuration;

	/**
	 * The main JXIO Process Task that runs.
	 * @param args
	 */
	public static void main(String[] args) {
		if (!argsCheck(args)) {
			return;
		} else {
			try {
				System.out.println("\nRunning process " + args[1] + "!");
	            new JXIOProcessTask(args[0], args[1]).call();
            } catch (Exception e) {
	            e.printStackTrace();
            }
		}
	}
	
	/**
     * Constructs a new JXIOProcessTask
     */
	public JXIOProcessTask(String storyFile, String processID) {
		this.storyFile = storyFile;
		this.processID = processID;
		this.story = new Story();

		// Read story
		read(new File(this.storyFile));
		// Read Chapter
		chapter = Chapter.readChapter(machines, processes, clients, servers);
		mapPlayers(chapter);
		// run process == processID
		machine = chapter.myMachine;
		process = Character.getCharacterFromListByAttribute(chapter.myProcesses, "id", this.processID);
		serverPlayers = chapter.processServerPlayers.get(process);
		clientPlayers = chapter.processClientPlayers.get(process);
		workers = chapter.processWorkerThreads.get(process);
		maxDuration = chapter.processMaxDuration.get(process);
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

			// Get Seed
			seed = Story.getSeed(docRead);
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
			machines = Character.getCharactersOfType("machines", story.getCharacters());
			// Get Processes
			processes = Character.getCharactersOfType("processes", story.getCharacters());
			// Get Clients
			clients = Character.getCharactersOfType("clients", story.getCharacters());
			// Get Servers
			servers = Character.getCharactersOfType("servers", story.getCharacters());

		} catch (ParserConfigurationException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (SAXException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (IOException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		}
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

			// Configure Worker Threads
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
			int counter = 1;
			try {
				String uriQueryStr = "";
				List<Character> clientServers = new ArrayList<Character>(); // Will store all the servers the client
				// goes throght. Used for throght. Used for
				// chosing a msg_pool.

				// Get client parameters
				int id = Integer.valueOf(client.getAttribute("id"));
				Character process = Character.getCharacterFromListByAttribute(processes, "id", client
				        .getAttribute("process"));
				int duration = Integer.valueOf(client.getAttribute("duration"));
				Character server = Character.getCharacterFromListByAttribute(servers, "id", client
				        .getAttribute("server"));
				int startDelay = Integer.valueOf(client.getAttribute("start_delay"));
				int tps = Integer.valueOf(client.getAttribute("tps"));
				int batch = Integer.valueOf(client.getAttribute("batch"));
				int repeats = Integer.valueOf(client.getAttribute("repeats"));
				int repeatDelay = Integer.valueOf(client.getAttribute("repeat_delay"));
				int violentExit = Integer.valueOf(client.getAttribute("violent_exit"));

				// Update client's servers list
				clientServers.add(server);

				// Get client hops
				List<Character> supportingCharacters = client.getSupportingCharacters();
				List<Character> hops = null;
				for (Character character : supportingCharacters) {
					if (character.getCharacterType().equals("hops")) {
						hops = character.getSupportingCharacters();
						break;
					}
				}

				// Updated URI with hops
				for (Character hop : hops) {
					String serverHopID = hop.getAttribute("server");
					Character serverHop = Character.getCharacterFromListByAttribute(servers, "id", serverHopID);
					String processHopID = serverHop.getAttribute("process");
					Character processHop = Character.getCharacterFromListByAttribute(processes, "id", processHopID);
					String machineHopID = processHop.getAttribute("machine");
					Character machineHop = Character.getCharacterFromListByAttribute(machines, "id", machineHopID);
					String hostnameHop = machineHop.getAttribute("address");
					String portHop = serverHop.getAttribute("port");

					// Add hop to URI suffix
					uriQueryStr += uriQueryStr.isEmpty() ? "?" : "&";
					uriQueryStr += "nextHop=" + hostnameHop + ":" + portHop;

					// Update client's servers list
					clientServers.add(serverHop);
				}

				// Choose msg_pool
				MsgPoolData server_pool = chooseServersMsgPool(clientServers);
				// Get client msgs by factoring the servers msg_pool
				// and crossing in/out sizes
				int count = server_pool.getCount() * Integer.valueOf(client.getAttribute("msg_count_factor_perc"))
				        / 100;
				int in = server_pool.getOutSize() * Integer.valueOf(client.getAttribute("msg_size_in_factor_perc"))
				        / 100;
				int out = server_pool.getInSize() * Integer.valueOf(client.getAttribute("msg_size_out_factor_perc"))
				        / 100;
				// Check for minimal size
				in = (in > Utils.HEADER_SIZE) ? in : Utils.HEADER_SIZE;
				out = (out > Utils.HEADER_SIZE) ? out : Utils.HEADER_SIZE;

				MsgPoolData pool = new MsgPoolData(count, in, out);

				if (batch > count)
					batch = count;

				// Resolve hostname and port
				Character serverProcess = Character.getCharacterFromListByAttribute(processes, "id", server
				        .getAttribute("process"));
				Character machine = Character.getCharacterFromListByAttribute(machines, "id", serverProcess
				        .getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				int port = Integer.valueOf(server.getAttribute("port"));
				int reject = Integer.valueOf(client.getAttribute("reject"));
				if (reject == 1) {
					uriQueryStr += uriQueryStr.isEmpty() ? "?" : "&";
					uriQueryStr += "reject=1";
				}

				// Create URI
				URI uri = new URI("rdma://" + hostname + ":" + port + "/" + uriQueryStr);

				// Update max duration
				if (startDelay + duration > maxDuration) {
					maxDuration = startDelay + duration;
				}

				// Add client
				// each client will have a his own seed which is a derivative of the story's seed.
				// repeating client will be added as sevreal sequential clients.
				int clientStartDelay;
				for (int i = 0; i < repeats + 1; i++){
					clientStartDelay = (duration + repeatDelay) * i + startDelay;
					ClientPlayer cp = new ClientPlayer(id, uri, clientStartDelay, duration, pool, tps, batch, violentExit, seed + counter * 17);
					clientPlayers.add(cp);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			counter++;
		}
		// Set process' ClientPlayers
		chapter.processClientPlayers.put(myProcess, clientPlayers);
		return maxDuration;
	}

	/**
     * Chooses a random miniaml msg_pool from a list a servers.
     * 
     * @param clientServers
     *            The servers the client goes through.
     * @return MsgPoolData of the choosen server.
     */
	private MsgPoolData chooseServersMsgPool(List<Character> clientServers) {
		MsgPoolData data;
		// Randomize a msg_pool from your server
		Character server = clientServers.get(0);
		List<Character> serverSupportingCharacters = server.getSupportingCharacters();
		List<Character> msgPoolsList = null;
		for (Character character : serverSupportingCharacters) {
			if (character.getCharacterType().equals("msg_pools")) {
				msgPoolsList = character.getSupportingCharacters();
			}
		}
		int randIndex = new Random().nextInt(msgPoolsList.size());
		Character msgPool = msgPoolsList.get(randIndex);
		int count = Integer.valueOf(msgPool.getAttribute("msg_pool_count"));
		int in = Integer.valueOf(msgPool.getAttribute("msg_pool_size_in"));
		int out = Integer.valueOf(msgPool.getAttribute("msg_pool_size_out"));
		// Check for a better msg_pool in a hoop along the way
		// Check tho congigure a MsgPoolData with mininal out size, maximal in size and miniaml count.
		for (Character hop : clientServers.subList(1, clientServers.size())) {
			// Get msg_pools
			serverSupportingCharacters = hop.getSupportingCharacters();
			msgPoolsList = null;
			for (Character character : serverSupportingCharacters) {
				if (character.getCharacterType().equals("msg_pools")) {
					msgPoolsList = character.getSupportingCharacters();
				}
			}
			// Check msg_pools
			for (Character pool : msgPoolsList) {
				int pool_count = Integer.valueOf(pool.getAttribute("msg_pool_count"));
				int pool_in = Integer.valueOf(pool.getAttribute("msg_pool_size_in"));
				int pool_out = Integer.valueOf(pool.getAttribute("msg_pool_size_out"));
				// Check for a smaller msg_pool count
				if (count > pool_count){
					count = pool_count;
				}
				// Check for a bigger msg_pool in size
				if (in < pool_in){
					in = pool_in;
				}
				// Check for a smaller msg_pool out size
				if (out > pool_out) {
					out = pool_out;
				}
			}
		}
		// Return proper MsgPoolData
		data = new MsgPoolData(count, in, out);
		return data;
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
			int counter = 1;
			try {
				// Get server parameters
				int id = Integer.valueOf(server.getAttribute("id"));
				int port = Integer.valueOf(server.getAttribute("port"));
				Character process = Character.getCharacterFromListByAttribute(processes, "id", server
				        .getAttribute("process"));
				int duration = Integer.valueOf(server.getAttribute("duration"));
				int numWorkers = Integer.valueOf(server.getAttribute("num_workers"));
				int delay = Integer.valueOf(server.getAttribute("delay"));
				int startDelay = Integer.valueOf(server.getAttribute("start_delay"));
				int violentExit = Integer.valueOf(server.getAttribute("violent_exit"));
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
				Character machine = Character.getCharacterFromListByAttribute(machines, "id", process
				        .getAttribute("machine"));
				String hostname = machine.getAttribute("address");
				URI uri = new URI("rdma://" + hostname + ":" + port + "/");

				// Update max duration
				if (startDelay + duration > maxDuration) {
					maxDuration = startDelay + duration;
				}

				// Add server
				ServerPortalPlayer sp = new ServerPortalPlayer(numWorkers, id, 0, uri, startDelay, duration,
				        chapter.processWorkerThreads.get(myProcess), msgPools, violentExit, seed + counter * 3);
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

	/**
     * The function that is invoked when running the forked process. Computes the total running time of the process (can
     * be used for benchmarking).
     */
	public Integer call() throws Exception {

		// Set starting time
		long startTime = System.nanoTime();

		// Run
		if (serverPlayers.size() == 0 && clientPlayers.size() == 0) {
			System.out.println("=> [Machine: " + machine.getAttribute("name") + "] Process "
			        + process.getAttribute("id") + " has no clients nor servers.");
			System.out.println("=====");
			System.out.println("Done!");
			System.out.println("=====");
		} else {
			System.out.println("=> [Machine: " + machine.getAttribute("name") + "] Process "
			        + process.getAttribute("id") + ": " + serverPlayers.size() + " servers " + clientPlayers.size()
			        + " clients\n");
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
			System.out.println("Done sleeping! Must kill all threads!");
			System.out.println("=====================================");

			workers.close();

			System.out.println("=============");
			System.out.println("Done killing!");
			System.out.println("=============");
		}
		// Set ending time
		long endTime = System.nanoTime();

		// return running time
		int runningTime = (int) ((endTime - startTime) / 1000000000);
		return runningTime;
	}
	
	/**
     * Checks to is if the number of arguments passed is valid.
     * 
     * @param args
     *            The command line arguments.
     * @return True if number of arguments is valid.
     */
	private static boolean argsCheck(String[] args) {
		if (args.length < 2) {
			System.out.println("[ERROR] Wrong number of arguments!\nFirst arugment needs to be the directory of the story XML file."
			        + "\nSecond arugment needs to be the process ID."
			        + "\nparameters: 'STORY_FILE PROCESS_ID'\n");
			return false;
		}
		return true;
	}
}