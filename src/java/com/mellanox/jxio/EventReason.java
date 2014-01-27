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
package com.mellanox.jxio;

public enum EventReason {
	SUCCESS(0), 
	NOT_SUPPORTED(1), 
	NO_BUFS(2), 
	CONNECT_ERROR(3), 
	ROUTE_ERROR(4), 
	ADDR_ERROR(5), 
	UNREACHABLE(6), 
	MSG_SIZE(7), 
	PARTIAL_MSG(8), 
	MSG_INVALID(9), 
	MSG_UNKNOWN(10), 
	SESSION_REFUSED(11), 
	SESSION_ABORTED(12), 
	SESSION_DISCONNECTED(13), 
	SESSION_REJECTED(14),
	SESSION_REDIRECTED(15),
	BIND_FAILED(16), 
	TIMEOUT(17), 
	IN_PROGRESS(18), 
	INVALID_VERSION(19), 
	NOT_SESSION(20), 
	OPEN_FAILED(21), 
	READ_FAILED(22), 
	WRITE_FAILED(23), 
	CLOSE_FAILED(24), 
	UNSUCCESSFUL(25), 
	MSG_CANCELED(26), 
	MSG_CANCEL_FAILED(27), 
	MSG_NOT_FOUND(28),
	MSG_FLUSHED(29);

	private int index;

	private EventReason(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	private static EventReason[] allReasons = values();

	public static EventReason getEventByIndex(int index) {
		return allReasons[index];
	}
}
