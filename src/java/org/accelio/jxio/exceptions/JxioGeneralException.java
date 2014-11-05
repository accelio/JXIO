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
package org.accelio.jxio.exceptions;

import java.io.IOException;

import org.accelio.jxio.EventReason;

@SuppressWarnings("serial")
public class JxioGeneralException extends IOException {
	EventReason reason;
	String      s;

	public JxioGeneralException(int reason, String methodName) {
		this.reason = EventReason.getEventByXioIndex(reason);
		formatString(methodName);
	}

	public JxioGeneralException(EventReason reason, String methodName) {
		this.reason = reason;
		formatString(methodName);
	}

	public EventReason getReason() {
		return reason;
	}

	public String toString() {
		return this.s;
	}

	private void formatString(String methodName) {
		this.s = "Got exception '" + this.reason.toString() + "' while calling '" + methodName + "'";
	}
}
