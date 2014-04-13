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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TimerList {

	private final static Log LOG = LogFactory.getLog(TimerList.class.getSimpleName());

	private Timer            head;

	public abstract static class Timer {

		TimerList          list;
		private Timer      next;
		private Timer      prev;

		private final long expiry;
		private final long startedAt;
		final long         durationMicroSec;

		// this is the users callback function to implement on timer expiry
		public abstract void onTimeOut();

		public Timer(long durationMicroSec) {
			this.durationMicroSec = durationMicroSec;
			this.startedAt = System.nanoTime() / 1000;
			this.expiry = startedAt + this.durationMicroSec;
			if (LOG.isTraceEnabled())
				LOG.trace("new " + this.toString() + " done (" + super.toString() + ")");
		}

		public String toString() {
			return "Timer(duration=" + this.durationMicroSec + ")";
		}

		public boolean isStarted() {
			// If Timer is inserted into a list that it is started
			return (list != null);
		}
	}

	// stop this timer event
	public void stop(Timer timer) {
		if (timer.isStarted() == true && timer.list == this) {
			synchronized (this) {
				remove(timer);
				LOG.debug(head.toString() + " stopped");
			}
		}
	}

	// start this timer by adding to list
	// returns true if new timer was added first in line (new wakeup duration)
	public boolean start(Timer timer) {
		boolean notify = false;
		synchronized (this) {
			if (timer.isStarted() == true) {
				if (timer.list != this)
					return notify;

				remove(timer); // remove and re-insert
			}

			Timer timer_iter;

			if (head != null) {
				// search for the insertion point.
				timer_iter = head;
				while (timer_iter.expiry <= timer.expiry) {

					if (timer_iter.next == null) {
						// insert at tail
						timer_iter.next = timer;
						timer.prev = timer_iter;
						timer.next = null;
						return notify;
					}
					timer_iter = timer_iter.next;
				}

				// insert us before current timer
				timer.next = timer_iter;
				if (timer_iter.prev == null) {
					timer.prev = null;
					head = timer;
					notify = true; // Need to notify that something has changed at the beginning of the list.
				} else {
					timer.prev = timer_iter.prev;
					timer_iter.prev.next = timer;
				}

				timer_iter.prev = timer;

			} else {
				// empty list, insert as first
				timer.next = null;
				timer.prev = null;
				head = timer;
				notify = true; // Need to notify that something has changed at the beginning of the list.
			}
			if (LOG.isTraceEnabled())
				LOG.trace(timer.toString() + " started");
			return notify;
		}
	}

	// This will check from expired timers, in which case it will activate their callback and remove the timer from the
	// list
	public void checkForTimeOuts() {
		synchronized (this) {
			long now = System.nanoTime() / 1000;

			// check for TimeOut event and remove the Timers
			while (head != null && head.expiry <= now) {
				if (LOG.isTraceEnabled())
					LOG.trace(head.toString() + " expired");
				head.onTimeOut(); // activate the callback
				remove(head);
			}
		}
	}

	// return the duration (micro-sec) until next expiry time, zero if Timers expiried in the past or -1 if no Timers
	// are pending.
	public long getWaitDuration() {
		synchronized (this) {
			long timeout = -1;
			if (head != null) {
				timeout = head.expiry - System.nanoTime() / 1000;
				if (timeout < 0)
					timeout = 0;
			}
			return timeout;
		}
	}

	// remove this timer from the queue.
	private void remove(Timer timer) {

		timer.list = null;

		if (timer == head) {
			head.prev = null;
			head = timer.next;
		} else {
			timer.prev.next = timer.next;
		}

		if (timer.next != null) {
			timer.next.prev = timer.prev;
		}
	}
}
