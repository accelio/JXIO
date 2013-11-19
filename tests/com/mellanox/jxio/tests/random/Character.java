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

public class Character {

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
	 * @param characterType A string representing the character's type.
	 */
	public void setCharacterType(String characterType) {
		this.characterType = characterType;
	}

	/**
	 * Add an attribute to the character.
	 * @param att The attribute name.
	 * @param value The attribute value.
	 */
	public void setAttribute(String att, String value) {
		attributes.put(att, value);
	}
	
	/**
	 * @param att A character attribute name.
	 * @return The value og the given attribute.
	 */
	public String getAttribute(String att) {
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
	 * @return A list of character types of all the character's
	 * supporting characters.
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
}
