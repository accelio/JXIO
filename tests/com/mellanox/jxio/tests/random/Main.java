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
package com.mellanox.jxio.tests.random;

import java.io.File;

import com.mellanox.jxio.tests.random.storyrunner.JXIOStoryRunner;
import com.mellanox.jxio.tests.random.storyrunner.StoryRunner;
import com.mellanox.jxio.tests.random.storyteller.StoryTeller;

public class Main {

	public static final int    ITERATIONS  = 1;
	public static String       xmlFileDir;
	private static String      xmlFileName;
	private static StoryTeller storyTeller;
	private static StoryRunner storyRunner = new JXIOStoryRunner();
	private static File        storyFile;
	private static long        seed;

	/**
     * The main program that runs the probability XML file through the StoryTeller in order to produce a story XML file.
     * 
     * @param args
     *            The command line arguments.
     */
	public static void main(String[] args) {

		// Check for valid arguments
		if (!argsCheck(args)) {
			return;
		} else {
			for (int i = 0; i < ITERATIONS; i++) {
				// Handle the XML file
				xmlFileDir = args[0];
				xmlFileName = args[1];
				// Get input seed or randomize one
				if (xmlFileName.contains("probability")) {
					seed = (args.length < 3 || args[2].equals("0")) ? System.nanoTime() : Long.valueOf(args[2]);
					print("**********************************\nStory Random Seed: " + seed
					        + "\n**********************************");
					// Create a new StoryTeller Instance
					File probabiltyFile = new File(xmlFileDir + "/" + xmlFileName);
					storyTeller = new StoryTeller(probabiltyFile, seed);
					// Tell Story
					storyTeller.read();
					storyTeller.write();
					print("Finised reading probability file.");
					// Define story file
					String storyFileName = "story_" + seed + ".xml";
					storyFile = new File(xmlFileDir + "/" + storyFileName);
				} else if (xmlFileName.contains("story")) {
					// Define story file
					storyFile = new File(xmlFileDir + "/" + xmlFileName);
				} else {
					print("[ERROR] Invalid file name. File name must contain 'probability' or 'story'.");
					return;
				}
				// Read story
				storyRunner.read(storyFile);
				// Run story
				storyRunner.run();
			}
		}
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
			print("[ERROR] Missing argument!\nFirst arugment needs to be the directory of the tests XML file."
			        + "\nSecond arugment needs to be the file name of the test XML file."
			        + "\nThis must be a file containing the word 'probability' or 'story'."
			        + "\nA Third arugment MAY be added as a seed (for random selections).");
			return false;
		}
		return true;
	}

	/**
     * Prints the given message to the screen.
     * 
     * @param str
     */
	public static void print(String str) {
		System.out.println("\n" + str + "\n");
	}
}
