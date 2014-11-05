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

package org.accelio.jxio;

import org.accelio.jxio.EventName;

/**
 * SessionEvents that are received by ClientSession, ServerSession and ServerPortal
 * 
 */
public enum EventName {
	/**
	 * SESSION_REJECT is received by ClientSession in case the server chose to reject the session
	 */
	SESSION_REJECT(0),

	/**
	 * SESSION_CLOSED received by ClientSession or ServerSession. This event is received after session was properly
	 * closed (close method is asynchronous). This event is received if either of the sides initiated the close
	 * or if there is internal error on either of the sides
	 */
	SESSION_CLOSED(1),

	/**
	 * PORTAL_CLOSED is received by ServerPortal. In case the user initiated close
	 * of ServerPortal when there are still ServerSessions that are listening on him, all those ServerSessions are
	 * closed. When all ServerSessions are closed, PORTAL_CLOSED event is received.
	 */
	PORTAL_CLOSED(2),

	/**
	 * SESSION_ERROR is received by ClientSession, ServerSession and ServerPortal.
	 */
	SESSION_ERROR(3);

	private int index;

	private EventName(int i) {
		index = i;
	}

	private static EventName[] allEvents = values();

	public static EventName getEventByIndex(int index) {
		return allEvents[index];
	}
}