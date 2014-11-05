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
package org.accelio.jxio.impl;

import org.accelio.jxio.Msg;

import org.accelio.jxio.impl.Event;

public class EventMsgError extends Event {

	private int reason;
	private Msg msg;

	public EventMsgError(int eventType, long id, Msg msg, int reason) {
		super(eventType, id);
		this.reason = reason;
		this.msg = msg;
	}

	public int getReason() {
		return reason;
	}

	public Msg getMsg() {
		return msg;
	}
}
