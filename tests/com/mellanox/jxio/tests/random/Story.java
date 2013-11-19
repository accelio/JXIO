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

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Story {

	private List<Character> charactersList = new ArrayList<Character>();

	/**
	 * Add a character to the story.
	 * @param character
	 */
	public void addCharacter(Character character) {
		charactersList.add(character);
	}
	
	/**
	 * @return A list of all the characters in the story.
	 */
	public List<Character> getCharacters() {
		return this.charactersList;
	}
	
	/**
	 * Retrieves all story characters in the probability file.
	 * 
	 * @param docRead A Document of an XML file to read.
	 * 
	 * @return A list of all the story characters.
	 */
	public static List<String> getStoryCharacters(Document docRead) {
		List<String> characterTypes = new ArrayList<String>();
		// Read root tag
		NodeList nodeList = docRead.getElementsByTagName("root");
		// Iterate over all occurrences
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				// Iterate over all sub-tags
				for (int j = 1; j < element.getChildNodes().getLength(); j++) {
					Node item = element.getChildNodes().item(j);
					// Check for data tags
					if (!item.getNodeName().equals("#text") && !item.getNodeName().equals("#comment")) {
						// Get attribute's name
						String att = item.getNodeName();
						characterTypes.add(att);
					}
				}
			}
		}
		return characterTypes;
	}
	
	/**
	 * Reads all occurrences of a specific tag and creates corresponding characters in the story.
	 * 
	 * @param docRead 
	 *            A Document of an XML file to read.
	 * @param tag
	 *            A specific tag to read for the XML file.         
	 */
	public static Character readCharacter(Document docRead, String characterTag) {
		Character character = new Character();
		// Read given tag
		NodeList nodeList = docRead.getElementsByTagName(characterTag.toString());
		// Iterate over all occurrences
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				// Create a new story character
				character.setCharacterType(characterTag);
				// Update character's attributes
				updateAttributes(element, character);
				// Return the character
				return character;
			}
		}
		return character;
	}
	
	/**
	 * Reads all sub-tags of a given element and and sets them as attributes to the given character.
	 * 
	 * @param element
	 *            The DOM element of the character.
	 * @param mainCharacter
	 *            The character to whom the attributes are assigned.
	 */
	private static void updateAttributes(Element element, Character mainCharacter) {
		// Iterate over all sub-tags
		for (int j = 1; j < element.getChildNodes().getLength(); j++) {
			Node item = element.getChildNodes().item(j);
			// Check for data tags
			if (!item.getNodeName().equals("#text") && !item.getNodeName().equals("#comment")) {
				// Get attribute's name
				String att = item.getNodeName();
				// Get attribute's value
				Node valueNode = item.getAttributes().getNamedItem("value");
				String value = null;
				// If no value found, as it is a title tag, recurse into it's sub-tags
				if (valueNode != null) {
					value = valueNode.getNodeValue();
					mainCharacter.setAttribute(att, value);
				} else {
					// Create a supporting character
					Character supportingCharacter = new Character();
					supportingCharacter.setCharacterType(att);
					// Update it's attributes
					updateAttributes((Element) item, supportingCharacter);
					// Save to main character
					mainCharacter.addSupportingCharacter(supportingCharacter);
				}
			}
		}
	}
	
	/**
	 * Converts a word in plural form to it's single form. This is used based on the assumption that there exists a
	 * field under the plural name tag called "single_amount".
	 * 
	 * @param docRead 
	 *            A Document of an XML file to read.
	 * @param plural
	 *            A string representing a word in plural form.
	 * @return A string representing the given word in single form.
	 */
	public static String singleFromPlural(Document docRead, String plural) {
		// Read the plural tag and get it's singular form from the amount tag
		String single = null;
		NodeList nodeList = docRead.getElementsByTagName(plural);
		// Iterate over all occurrences
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				// Iterate over all sub-tags
				for (int j = 1; j < element.getChildNodes().getLength(); j++) {
					Node item = element.getChildNodes().item(j);
					// Check for data tags
					String att = item.getNodeName();
					if (att.contains("_amount")) {
						single = att.substring(0, att.indexOf("_amount"));
					}
				}
			}
		}
		return single;
	}
}
