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

import org.accelio.jxio.impl.Event;

public class EventNewSession extends Event {
	private long ptrSes;
	private String uri;
	private String srcIP;

	public EventNewSession(int eventType, long id, long ptr, String uri, String ip) {
		super(eventType, id); 
		this.ptrSes = ptr;
		this.uri = uri;
		this.srcIP = ip;
	}

	public long getPtrSes() {
		return ptrSes;
	}

	public String getUri() {
		return uri;
	}

	public String getSrcIP() {
		return srcIP;
	}
}
