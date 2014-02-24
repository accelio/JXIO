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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.mellanox.jxio.tests.random.Character;
import com.mellanox.jxio.tests.random.Main;
import com.mellanox.jxio.tests.random.Story;

public class JXIOStoryRunner implements StoryRunner {

	private final static Log LOG    = LogFactory.getLog(JXIOStoryRunner.class.getSimpleName());

	private File             storyFile;
	private Document         docRead;
	private Story            story;
	private long             seed;
	private Chapter          chapter;
	private List<String>     characterTypes;
	private List<Character>  machines;
	private List<Character>  processes;
	private List<Character>  servers;
	private List<Character>  clients;
	private int              status = 1;

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
			this.storyFile = storyFile;
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

			// Print Summary
			System.out.println("Finished reading the story.");
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
		chapter = Chapter.readChapter(machines, processes, clients, servers);
		if (chapter == null) {
			System.out.println("[ERROR] Failed to read chapter.");
			status = 0; // Change to "= 1" after mahcines configuration happens automaticlly
			return;
		} else if (chapter.myProcesses.size() == 0) {
			status = 0;
			return;
		} else {
			// Run
			System.out.println("\nRunning JXIO processes...\n");
			final String dir = new File(".").getAbsolutePath();
			final String topDir = new File("..").getAbsolutePath();
			final String log4jFile = "com/mellanox/jxio/tests/random/storyrunner/log4j.properties.randomtest";
			Map<Process, BufferedReader> jxioProcesses = new HashMap<Process, BufferedReader>();
			for (Character process : chapter.myProcesses) {
				try {
					// Code coverage configuration
					String coberturaJarPath = Main.coberturaJarPath + ":"; // "/.autodirect/acclgwork/general/cobertura-2.0.3/cobertura-2.0.3.jar:";
					String javaCoverageProps = Main.javaCoverageProps; // "-Dnet.sourceforge.cobertura.datafile=/tmp/mars_tests/UDA-jx.db/tests/covfile_cobertura.ser";
					// Configure process
					ProcessBuilder jxioProcessBuilder = new ProcessBuilder("java", "-Dlog4j.configuration=" + log4jFile
					        + "_" + process.getAttribute("log_level"), "-cp", coberturaJarPath + topDir
					        + "/bin/jxio.jar:" + topDir + "/src/lib/commons-logging.jar:" + topDir
					        + "/src/lib/log4j-1.2.15.jar:" + dir);
					if (!javaCoverageProps.equals("")) {
						jxioProcessBuilder.command().add(javaCoverageProps);
					}
					jxioProcessBuilder.command().add("com.mellanox.jxio.tests.random.storyrunner.JXIOProcessTask");
					// Add JXIO process parameters
					jxioProcessBuilder.command().add(storyFile.getAbsolutePath());
					jxioProcessBuilder.command().add(process.getAttribute("id"));
					// Configure log
					File processLog = new File("jxio_process_" + process.getAttribute("id") + "_log.txt");
					processLog.delete();
					jxioProcessBuilder.redirectOutput(processLog);
					jxioProcessBuilder.directory(new File(".").getAbsoluteFile());
					// Start process
					Process jxioProcess = jxioProcessBuilder.start();
					// Handle process output
					BufferedReader buffer = new BufferedReader(new InputStreamReader(jxioProcess.getInputStream()));
					// Store process
					jxioProcesses.put(jxioProcess, buffer);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Wait for all process to finish
			for (Process p : jxioProcesses.keySet()) {
				try {
					p.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			// Print all process logs to screen and check for errors
			status = 0; // Set run status to successful
			for (Character process : chapter.myProcesses) {
				String processID = process.getAttribute("id");
				System.out.println("\n==================");
				System.out.println("Log for process " + processID);
				System.out.println("==================\n");
				BufferedReader br;
				try {
					br = new BufferedReader(new FileReader("jxio_process_" + processID + "_log.txt"));
					String line = null;
					while ((line = br.readLine()) != null) {
						System.out.println(line);
						// Check if an error occured
						if (line.toLowerCase().contains("core dump") || line.toLowerCase().contains("fatal error")
						        || line.contains("FAILURE")) {
							status = 1; // Set run status to failure
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println("\nAll JXIO processes ended!\n");
		}
	}

	/**
     * Returns true if the story ran successfully.
     */
	public boolean wasRunSuccessful() {
		return ((status == 0) ? true : false);
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
