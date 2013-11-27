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

public class MsgPoolData {
	private final int count;
	private final int inSize;
	private final int outSize;

	MsgPoolData(int count, int in, int out) {
		this.count = count;
		this.inSize = in;
		this.outSize = out;
	}

	public int getCount() {
		return count;
	}

	public int getInSize() {
		return inSize;
	}

	public int getOutSize() {
		return outSize;
	}

}
