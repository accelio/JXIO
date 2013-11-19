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
package com.mellanox.jxio.tests.random.storyteller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.mellanox.jxio.tests.random.Character;
import com.mellanox.jxio.tests.random.Main;
import com.mellanox.jxio.tests.random.Story;

public class StoryTeller {

	private final File           xmlFile;
	private Document             docRead;
	private Document             docWrite;
	private Story                story;
	private Map<String, Integer> counters;
	private List<String>         characterTypes;
	private long                 seed   = -1;
	private Random               random = new Random();

	/**
	 * Constructs an new StoryTeller.
	 * 
	 * @param xmlFile
	 *            The probability XML file.
	 */
	public StoryTeller(File xmlFile) {
		this.story = new Story();
		this.counters = new HashMap<String, Integer>();
		this.xmlFile = xmlFile;
	}

	/**
	 * Constructs an new StoryTeller with a fixed random seed.
	 * 
	 * @param xmlFile
	 *            The probability XML file.
	 * @param seed
	 *            A long that represents the initial seed.
	 */
	public StoryTeller(File xmlFile, long seed) {
		this(xmlFile);
		this.seed = seed;
		this.random = new Random(seed);
	}

	/**
	 * Reads the probability XML file.
	 */
	public void read() {
		try {
			// Set needed XML document handling objects
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			docRead = dBuilder.parse(this.xmlFile);
			docRead.getDocumentElement().normalize();

			// Get characters
			characterTypes = Story.getStoryCharacters(docRead);

			// Read all character types
			for (String characterType : characterTypes) {
				Character character = Story.readCharacter(docRead, characterType);
				story.addCharacter(character);
				
			}
		} catch (ParserConfigurationException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (SAXException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		} catch (IOException e) {
			System.out.println("[ERROR] XML Read Expetion occurred.");
		}		
	}

	/**
	 * Randomizes a set of values based on the defined probabilities.
	 * 
	 * @param nodeValue
	 *            A list of [PROBABILITY]:RANGE pairs, [PROBABILITY]:VALUE pairs, just RANGE (equivalent to [100]:RANGE)
	 *            or just VALUE (equivalent to [100]:VALUE).
	 * @return A String representing a single random number value.
	 */
	private String randomizeValueByProbability(String nodeValue) {
		// Do only if this is a probability defined value
		if (nodeValue.contains(":")) {
			List<Item<String>> items = new ArrayList<Item<String>>();
			String[] nodeValues = nodeValue.split(",");
			// Create an Item list of possible values and there probability
			for (String pair : nodeValues) {
				// Update probability
				int probability = Integer.parseInt(pair.substring(pair.indexOf("[") + 1, pair.indexOf("]")));
				// Update a value (within range if needed)
				String value = randomaizeRange(pair.substring(pair.indexOf(":") + 1));
				// Add Item to list
				items.add(new Item<String>(value, probability));
			}
			RandomSelector<String> rs = (seed == -1) ? new RandomSelector<String>(items) : new RandomSelector<String>(items, random);
			// Randomize a value by the probabilities given
			return rs.getRandom().getValue();

		}
		// Check the non-probability defined value for range and return
		return randomaizeRange(nodeValue);
	}

	/**
	 * Randomizes a number within a range of numbers.
	 * 
	 * @param range
	 *            A String representing a range between two numbers, formated as two integers separated by a hyphen.
	 *            E.g., "3-6".
	 * @return A String representing a single random number value.
	 */
	private String randomaizeRange(String range) {
		// Do only if this is indeed a range
		if (range.contains("-")) {
			String[] edges = range.split("-");
			int min = Integer.parseInt(edges[0]);
			int max = Integer.parseInt(edges[1]);
			int remainder = max - min;
			int rand = (remainder <= 0) ? max : random.nextInt(remainder) + min;
			return String.valueOf(rand);
		}
		// Return the simple value
		return range;
	}

	/**
	 * Writes the story XML file.
	 */
	public void write() {
		// Instance of a DocumentBuilderFactory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			// Use factory to get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
			// Create instance of DOM
			docWrite = db.newDocument();
			// Create the root element
			Element rootEle = docWrite.createElement("root");
			// Create data elements and place them under root
			// Generate story
			generateStoryTag(rootEle);
			// Generate characters and supporting characters
			for (Character character : story.getCharacters()) {
				String single = Story.singleFromPlural(docRead, character.getCharacterType());
				generateTag(rootEle, character, counters.get(single + "_amount"));
			}

			// Finish document
			docWrite.appendChild(rootEle);

			try {
				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				//tr.setOutputProperty(OutputKeys.METHOD, "xml"); NOT NEEDED
				//tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); NOT NEEDED
				tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); // Remove XML Header

				// Send DOM to file
				tr.transform(new DOMSource(docWrite), new StreamResult(new FileOutputStream(Main.xmlFileDir
				        + "/new_story.xml")));

			} catch (TransformerException te) {
				System.out.println("[ERROR] " + te.getMessage());
			} catch (IOException ioe) {
				System.out.println("[ERROR] " + ioe.getMessage());
			}
		} catch (ParserConfigurationException pce) {
			System.out.println("[ERROR] Error trying to instantiate DocumentBuilder " + pce);
		}
	}

	/**
	 * Generates the story tag in the story XML file.
	 * 
	 * @param rootEle
	 *            The root DOM element of the file.
	 */
	private void generateStoryTag(Element rootEle) {
		Element e, sub_e;
		// Generate story
		e = docWrite.createElement("story");
		// Generate probability file tag
		sub_e = docWrite.createElement("probability_file");
		sub_e.setAttribute("value", xmlFile.toString());
		e.appendChild(sub_e);
		// Generate seed if set
		if (seed != -1) {
			sub_e = docWrite.createElement("seed");
			sub_e.setAttribute("value", String.valueOf(seed));
			e.appendChild(sub_e);
		}
		// Generate numbers of characters based on the amount field value
		for (String characterType : characterTypes) {
			generateNumberTag(e, Story.singleFromPlural(docRead, characterType) + "_amount");
		}
		// Finish Story
		rootEle.appendChild(e);
	}

	/**
	 * Generates a character's amount tag under the story XML file.
	 * 
	 * @param e
	 *            A DOM element in the file.
	 * @param tag
	 *            The character type in single form.
	 */
	private void generateNumberTag(Element e, String tag) {
		String value = docRead.getElementsByTagName(tag).item(0).getAttributes().getNamedItem("value").getNodeValue();
		String randomValue = randomizeValueByProbability(value);
		// Add to numbers storage
		counters.put(tag, Integer.parseInt(randomValue));
		// Add to XML
		Element sub_e = docWrite.createElement(tag);
		sub_e.setAttribute("value", randomValue);
		e.appendChild(sub_e);
	}

	/**
	 * Generates a character's tags.
	 * 
	 * @param rootEle
	 *            A DOM element in the file.
	 * @param mainCharacter
	 *            The character for whom a tag is created.
	 * @param numOccurrences
	 *            The number of repeated occurrences needed to be create for the given character.
	 */
	private void generateTag(Element rootEle, Character mainCharacter, int numOccurrences) {

		Element e, single_e, sub_e;
		String rootTitle = mainCharacter.getCharacterType();
		String singleTitle = Story.singleFromPlural(docRead, rootTitle);

		if (singleTitle == null) {
			// Generate attributes (excluding inner tags)
			for (String att : mainCharacter.getAttributes().keySet()) {
				if (!att.contains(singleTitle + "_amount")) {
					single_e = docWrite.createElement(att);
					String value;
					if (mainCharacter.getAttribute(att).equals("random")) {
						value = randomaizeRange("1-" + counters.get(att + "_amount"));
					} else {
						value = randomizeValueByProbability(mainCharacter.getAttribute(att)).trim();
					}
					single_e.setAttribute("value", value);
					rootEle.appendChild(single_e);
				}
			}
		} else {
			// Generate root
			e = docWrite.createElement(rootTitle);
			int id = 1;
			for (int i = 0; i < numOccurrences; i++) {
				single_e = docWrite.createElement(singleTitle);
				e.appendChild(single_e);
				// Generate ID
				sub_e = docWrite.createElement("id");
				sub_e.setAttribute("value", String.valueOf(id));
				id++;
				single_e.appendChild(sub_e);

				// Generate attributes (excluding inner tags)
				for (String att : mainCharacter.getAttributes().keySet()) {
					if (!att.contains(singleTitle + "_amount")) {
						sub_e = docWrite.createElement(att);
						String value;
						if (mainCharacter.getAttribute(att).equals("random")) {
							value = randomaizeRange("1-" + counters.get(att + "_amount"));
						} else {
							value = randomizeValueByProbability(mainCharacter.getAttribute(att)).trim();
						}
						sub_e.setAttribute("value", value);
						single_e.appendChild(sub_e);
					}
				}
				// Generate inner tags
				for (Character supportingCharacter : mainCharacter.getSupportingCharacters()) {
					String occurrences = supportingCharacter.getAttribute(Story.singleFromPlural(docRead, supportingCharacter
					        .getCharacterType()) + "_amount");
					// Check if amount is configured
					if (occurrences != null) {
						occurrences = randomizeValueByProbability(occurrences);
						generateTag(single_e, supportingCharacter, Integer.valueOf(occurrences));
					} else {
						// This is a fixed tag, no need to make several occurrences
						generateTag(single_e, supportingCharacter, 0);
						// Remove the supporting character as it is to be shown once
						mainCharacter.getSupportingCharacters().remove(supportingCharacter);
						// Stop to loop from continuing the other supporting characters
						break;
					}
				}
			}
			// Finish
			rootEle.appendChild(e);
		}
	}
}
