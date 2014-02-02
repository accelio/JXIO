package com.mellanox.jxio.tests.random.storyrunner;

import java.util.List;
import java.util.Map;

import com.mellanox.jxio.tests.random.Character;
import com.mellanox.jxio.tests.random.storyrunner.ClientPlayer;
import com.mellanox.jxio.tests.random.storyrunner.ServerPortalPlayer;
import com.mellanox.jxio.tests.random.storyrunner.Utils;
import com.mellanox.jxio.tests.random.storyrunner.WorkerThreads;

/**
 * A chapter from the story, segment of the story matching a machine.
 */
public class Chapter {
	public String                                   myIP;
	public Character                                myMachine;
	public List<Character>                          myProcesses;
	public Map<Character, List<Character>>          processServers;
	public Map<Character, List<Character>>          processClients;
	public Map<Character, List<ServerPortalPlayer>> processServerPlayers;
	public Map<Character, List<ClientPlayer>>       processClientPlayers;
	public Map<Character, Integer>                  processMaxDuration;
	public Map<Character, WorkerThreads>            processWorkerThreads;

	
	public static Chapter readChapter(List<Character> machines, List<Character> processes, List<Character> clients, List<Character> servers){

		Chapter myChapter = new Chapter();

		// Get my IP
		myChapter.myIP = Utils.getIP();
		if (myChapter.myIP == null) {
			System.out.println("[ERROR] No Infiniband found on this machine!");
			return null;
		}
		System.out.println("Your machine's IP is " + myChapter.myIP);

		// Fetch my machine
		myChapter.myMachine = Character.getCharacterFromListByAttribute(machines, "address", myChapter.myIP);
		if (myChapter.myMachine == null) {
			System.out.println("[ERROR] No machine with your IP found in the story!");
			return null;
		}
		System.out.println("Your name is " + myChapter.myMachine.getAttribute("name"));

		// Fetch my processes
		myChapter.myProcesses = Character.getCharactersFromListByAttribute(processes, "machine", myChapter.myMachine
		        .getAttribute("id"));
		if (myChapter.myProcesses.size() == 0) {
			System.out.println("No process found for your machine!");
		} else {
			System.out.println("You have " + myChapter.myProcesses.size() + " processes to run.");

			// Map
			// Map servers To Processes
			myChapter.processServers = Character.mapCharactersByCharacter(servers, "process", processes);
			// Map clients To Processes
			myChapter.processClients = Character.mapCharactersByCharacter(clients, "process", processes);
		}
		return myChapter;
	}
}

