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

public class Item<T> {

	private T   value;
	private int probability;

	/**
	 * Constructs a new Item.
	 * @param value The item generic value.
	 * @param probability The value's probability.
	 */
	public Item(T value, int probability) {
		this.value = value;
		this.probability = probability;
	}

	/**
	 * @return The item's value.
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Set the item value to the given one.
	 * @param value
	 */
	public void setValue(T value) {
		this.value = value;
	}

	/**
	 * @return The probability of the item's value.
	 */
	public int getProbability() {
		return probability;
	}

	/**
	 * Set the probability of the item's value to the given one.
	 * @param probability
	 */
	public void setProbability(int probability) {
		this.probability = probability;
	}
}
