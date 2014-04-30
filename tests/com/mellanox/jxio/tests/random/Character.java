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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Character {

	private final static Log    LOG                  = LogFactory.getLog(Character.class.getSimpleName());

	private String              characterType;
	private Map<String, String> attributes           = new HashMap<String, String>();
	private List<Character>     supportingCharacters = new ArrayList<Character>();

	/**
     * @return The character's type
     */
	public String getCharacterType() {
		return characterType;
	}

	/**
     * Set the character's type to the given one.
     * 
     * @param characterType
     *            A string representing the character's type.
     */
	public void setCharacterType(String characterType) {
		this.characterType = characterType;
	}

	/**
     * Add an attribute to the character.
     * 
     * @param att
     *            The attribute name.
     * @param value
     *            The attribute value.
     */
	public void setAttribute(String att, String value) {
		attributes.put(att, value);
	}

	/**
     * @param att
     *            A character attribute name.
     * @return The value of the given attribute.
     */
	public String getAttribute(String att) {
		if (attributes.get(att) == null) {
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			LOG.warn("Retrieved attribute is NULL!");
			LOG.warn("This happed when " + stackTrace[2].getClassName() + " called " + stackTrace[1].getMethodName()
			        + " in line " + stackTrace[2].getLineNumber() + " for the attribute '" + att + "'.");
		}
		return attributes.get(att);

	}

	/**
     * @return A Map of the character's attributes and their values.
     */
	public Map<String, String> getAttributes() {
		return this.attributes;
	}

	/**
     * @return A list of all the character's supporting characters.
     */
	public List<Character> getSupportingCharacters() {
		return supportingCharacters;
	}

	/**
     * @return A list of character types of all the character's supporting characters.
     */
	public List<String> getSupportingCharactersTypes() {
		List<String> supportingCharactersTypes = new ArrayList<String>();
		for (Character character : supportingCharacters) {
			supportingCharactersTypes.add(character.getCharacterType());
		}
		return supportingCharactersTypes;
	}

	/**
     * Add a a supporting character to this character.
     * 
     * @param supportingCharacter
     */
	public void addSupportingCharacter(Character supportingCharacter) {
		supportingCharacters.add(supportingCharacter);
	}

	public String toString() {
		String str = "Character Type: " + characterType.toString();
		for (String att : attributes.keySet()) {
			str += ", " + att + ": " + attributes.get(att);
		}
		return str;
	}

	/**
     * Retrieve all characters matching a specific type.
     * 
     * @param charcterType
     *            A character type in single form.
     * @return A list of all characters of the requested type.
     */
	public static List<Character> getCharactersOfType(String charcterType, List<Character> charactersList) {
		List<Character> chracterList = new ArrayList<Character>();
		for (Character character : charactersList) {
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
	public static Map<Character, List<Character>> mapCharactersByCharacter(List<Character> supportingCharacter,
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
     *            The attribute to match.
     * @param value
     *            The value of the requested attribute to match.
     * @return The character from the list with there attribute matching the given value.
     */
	public static Character getCharacterFromListByAttribute(List<Character> characters, String attribute, String value) {
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
     *            The attribute to match.
     * @param value
     *            The value of the requested attribute to match.
     * @return A list of character from the list with there attribute matching the given value.
     */
	public static List<Character> getCharactersFromListByAttribute(List<Character> characters, String attribute,
	        String value) {
		List<Character> list = new ArrayList<Character>();
		for (Character character : characters) {
			if (character.getAttribute(attribute).equals(value)) {
				list.add(character);
			}
		}
		return list;
	}
}
