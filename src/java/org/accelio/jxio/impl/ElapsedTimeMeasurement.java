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

public class ElapsedTimeMeasurement {

	private long startTimeNano = 0;
	
	public ElapsedTimeMeasurement() {
		resetStartTime();
	}

	public long getNowNano() {
		return System.nanoTime();
	}

	public void resetStartTime() {
		startTimeNano = getNowNano();
	}

	public boolean isTimeOutNano(long durationTimeNano) {
		return (durationTimeNano < getElapsedTimeNano());
	}

	public boolean isTimeOutMicro(long durationTimeMicro) {
		return (durationTimeMicro < getElapsedTimeMicro());
	}

	public boolean isTimeOutMilli(long durationTimeMilli) {
		return (durationTimeMilli < getElapsedTimeMilli());
	}

	public long getElapsedTimeNano() {
		return getNowNano() - startTimeNano;
	}

	public long getElapsedTimeMicro() {
		return getElapsedTimeNano()/1000L;
	}

	public long getElapsedTimeMilli() {
		return getElapsedTimeNano()/1000000L;
	}
}
