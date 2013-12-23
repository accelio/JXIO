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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.impl.Bridge;
import com.mellanox.jxio.impl.ElapsedTimeMeasurement;
import com.mellanox.jxio.impl.Event;
import com.mellanox.jxio.impl.EventMsgError;
import com.mellanox.jxio.impl.EventNewMsg;
import com.mellanox.jxio.impl.EventNewSession;
import com.mellanox.jxio.impl.EventSession;
import com.mellanox.jxio.impl.EventSessionEstablished;

/**
 *
 */
public class EventQueueHandler implements Runnable {

	private final long             refToCObject;
	private final int              eventQueueSize        = 15000; // size of byteBuffer
	private int                    eventsWaitingInQ      = 0;
	private ByteBuffer             eventQueue            = null;
	private ElapsedTimeMeasurement elapsedTime           = null;
	private Map<Long, Eventable>   eventables            = new HashMap<Long, Eventable>();
	private Map<Long, Msg>         msgsPendingReply      = new HashMap<Long, Msg>();
	private Map<Long, Msg>         msgsPendingNewRequest = new HashMap<Long, Msg>();
	private volatile boolean       breakLoop             = false;
	private volatile boolean       stopLoop              = false;
	private volatile boolean       inRunLoop             = false;
	private static final Log       LOG                   = LogFactory
	                                                             .getLog(EventQueueHandler.class.getCanonicalName());

	// ctor
	public EventQueueHandler() {
		DataFromC dataFromC = new DataFromC();
		boolean statusError = Bridge.createCtx(eventQueueSize, dataFromC);
		if (statusError) {
			LOG.error("there was an error creating ctx on c side!");
		}
		this.eventQueue = dataFromC.eventQueue;
		this.refToCObject = dataFromC.ptrCtx;
		this.elapsedTime = new ElapsedTimeMeasurement();
	}

	/**
	 * Entry point for Thread.start() implementation from Runnable interfaces
	 */
	public void run() {
		while (!this.stopLoop) {
			runEventLoop(-1 /* Infinite events */, -1 /* Infinite duration */);
		}
	}

	/**
	 * Stops the running thread which is blocked on the run() interface
	 */
	public void stop() {
		this.stopLoop = true;
		breakEventLoop();
	}

	/**
	 * Main progress engine thread entry point.
	 * This function will cause all depending objects callbacks to be activated respectfully on new event occur.
	 * the calling thread will block for 'maxEvents' or a total duration of 'timeOutMicroSec.
	 * 
	 * @param maxEvents
	 *            : function will block until processing max events (callbacks) before returning or the timeout reached
	 *            use '-1' for infinite number of events
	 * @param timeOutMicroSec
	 *            : function will block until max duration of timeOut (measured in micro-sec) or maxEvents reached
	 *            use '-1' for infinite duration
	 * @return number of events processes or zero if timeout
	 */
	public int runEventLoop(int maxEvents, long timeOutMicroSec) {
		if (getId() == 0) {
			LOG.error("no context opened on C side. can not run event loop");
			return 0;
		}
		if (this.inRunLoop){
			LOG.error(this.toString() + " event loop is already running");
			return 0;
		}
		this.inRunLoop = true;

		boolean is_forever = (timeOutMicroSec == -1) ? true : false;
		boolean is_infinite_events = (maxEvents == -1) ? true : false;

		this.elapsedTime.resetStartTime();
		int eventsHandled = 0;

		while (!this.breakLoop && ((is_infinite_events) || (maxEvents > eventsHandled))
		        && ((is_forever) || (!this.elapsedTime.isTimeOutMicro(timeOutMicroSec)))) {

			if (LOG.isDebugEnabled()) {
				LOG.debug("["
				        + getId()
				        + "] in loop with "
				        + eventsWaitingInQ
				        + " events in Q. handled "
				        + eventsHandled
				        + " events out of "
				        + maxEvents
				        + ", "
				        + "elapsed time is "
				        + this.elapsedTime.getElapsedTimeMicro()
				        + " usec (blocking for "
				        + ((is_forever) ? "infinite duration)" : "a max duration of " + timeOutMicroSec / 1000
				                + " msec.)"));
			}

			if (eventsWaitingInQ <= 0) { // the event queue is empty now, get more events from libxio
				eventQueue.rewind();
				eventsWaitingInQ = Bridge.runEventLoop(getId(), timeOutMicroSec);
			}

			// process in eventQueue pending events
			if (eventsWaitingInQ > 0) {
				handleEvent(eventQueue);
				eventsHandled++;
				eventsWaitingInQ--;
			}
		}

		this.breakLoop = false;
		if (LOG.isDebugEnabled()) {
			LOG.debug("[" + getId() + "] returning with " + eventsWaitingInQ + " events in Q. handled " + eventsHandled
			        + " events, elapsed time is " + elapsedTime.getElapsedTimeMicro() + " usec.");
		}
		this.inRunLoop = false;
		return eventsHandled;
	}

	/**
	 * Main progress engine thread break point.
	 * Calling this function will force the runEventLoop() function to return when possible,
	 * no matter the number of events or duration it still should be blocking.
	 * 
	 * This function can be called from any thread context
	 */
	public void breakEventLoop() {
		if (getId() == 0) {
			LOG.error("no context opened on C side. can not break event loop");
			return;
		}
		if (this.breakLoop == false) {
			this.breakLoop = true;

			Bridge.breakEventLoop(getId());
		}
	}

	/**
	 * Close (and stops) this EQH and release all corresponding Java and Native resources
	 * (including closing the related SM, SS & CS)
	 * 
	 * This function Should be called only once no other thread is inside the runEventLoop()
	 */
	public void close() {
		if (getId() == 0) {
			LOG.error("no context opened on C side. can not close event loop");
			return;
		}
		if (this.inRunLoop){
			LOG.error(this.toString() + " can not close EQH from within runEventLoop");
			return;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("[" + getId() + "] closing EQH ");
		}
		while (!this.eventables.isEmpty()) {
			int waitForEvent = 0;
			Iterator<Map.Entry<Long, Eventable>> it = this.eventables.entrySet().iterator();
			while (it.hasNext()) {
				Eventable ev = it.next().getValue();
				if (!ev.getIsClosing()) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("[" + getId() + "] closing eventable with refToCObject " + ev.getId());
					}
					ev.close();
					it = this.eventables.entrySet().iterator();
				}
				if (ev.getIsExpectingEventAfterClose()) {
					waitForEvent++;
				}
			}
			if (waitForEvent != 0) {
				runEventLoop(waitForEvent, -1);
			}
			LOG.warn("attempting to close EQH while objects " + this.eventables.keySet() + " are still listening.");
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("[" + getId() + "] no more objects listening");
		}
		Bridge.closeCtx(getId());
		this.stopLoop = true;
		if (LOG.isDebugEnabled()) {
			LOG.debug("[" + getId() + "] closing EQH is finished");
		}
	}

	static abstract class Eventable {

		private long    id        = 0;
		private boolean isClosing = false; // indicates that this class is in the process of releasing it's resources

		/*
		 * enum eventType {
		 * sessionError, msgError, sessionEstablished, msgRecieved,
		 * newSession
		 * }
		 */

		final long getId() {
			return id;
		}

		void setId(final long id) {
			if (this.id == 0)
				this.id = id;
			// TODO: 'else throw' exception instead of final member 'refToCObject'
		}

		public abstract boolean close();

		boolean getIsClosing() {
			return isClosing;
		}

		void setIsClosing(boolean isClosing) {
			this.isClosing = isClosing;
		}

		abstract void onEvent(Event ev);

		// indicates that after Eventable.close() an event should arrive on eventloop
		abstract boolean getIsExpectingEventAfterClose();
	}

	long getId() {
		return refToCObject;
	}

	void addEventable(Eventable eventable) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("** adding " + eventable.getId() + " to map of EQH id=" + this.getId());
		}
		// add lock
		synchronized (eventables) {
			if (eventable.getId() != 0) {
				eventables.put(eventable.getId(), eventable);
			}
		}
	}

	void removeEventable(Eventable eventable) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("** removing " + eventable.getId() + " from map of EQH id=" + this.getId());
		}
		synchronized (eventables) {
			eventables.remove(eventable.getId());
		}
	}

	void addMsgInUse(Msg msg) {
		if (msg.getId() != 0) {
			msgsPendingReply.put(msg.getId(), msg);
		}
	}

	Msg getAndremoveMsgInUse(long id) {
		Msg msg = msgsPendingReply.get(id);
		msgsPendingReply.remove(id);
		return msg;
	}

	private void handleEvent(ByteBuffer eventQueue) {

		Eventable eventable;
		int eventType = eventQueue.getInt();
		long id = eventQueue.getLong();
		switch (eventType) {

			case 0: // session error event
			{
				int errorType = eventQueue.getInt();
				int reason = eventQueue.getInt();
				EventSession evSes = new EventSession(eventType, id, errorType, reason);
				synchronized (eventables) {
					eventable = eventables.get(id);
				}
				if (eventable == null) {
					LOG.warn(this.toString() + " eventable with id " + id + " was not found in map");
					break;
				}
				eventable.onEvent(evSes);
			}
				break;

			case 1: // msg error
			{
				EventMsgError evMsgErr = new EventMsgError(eventType, id);
				synchronized (eventables) {
					eventable = eventables.get(id);
				}
				if (eventable == null) {
					LOG.warn(this.toString() + " eventable with id " + id + " was not found in map");
					break;
				}
				eventable.onEvent(evMsgErr);
			}
				break;

			case 2: // session established
			{
				EventSessionEstablished evSesEstab = new EventSessionEstablished(eventType, id);
				eventable = eventables.get(id);
				if (eventable == null) {
					LOG.warn(this.toString() + " eventable with id " + id + " was not found in map");
					break;
				}
				eventable.onEvent(evSesEstab);
			}
				break;

			case 3: // on request
			{
				Msg msg = this.msgsPendingNewRequest.get(id);
				final int msg_size = eventQueue.getInt();
				msg.getIn().limit(msg_size);
				final long session_id = eventQueue.getLong();
				if (LOG.isTraceEnabled()) {
					LOG.trace("session refToCObject" + session_id);
				}
				eventable = eventables.get(session_id);
				if (eventable == null) {
					LOG.warn(this.toString() + " eventable with id " + session_id + " was not found in map");
					break;
				}
				EventNewMsg evMsg = new EventNewMsg(eventType, id, msg);
				eventable.onEvent(evMsg);
			}
				break;

			case 4: // on reply
			{
				Msg msg = msgsPendingReply.remove(id);
				final int msg_size = eventQueue.getInt();
				msg.getIn().limit(msg_size);
				if (LOG.isTraceEnabled()) {
					LOG.trace("msg is " + msg);
				}
				EventNewMsg evMsg = new EventNewMsg(eventType, id, msg);
				eventable = msg.getClientSession();
				if (LOG.isTraceEnabled()) {
					LOG.trace("eventable is " + eventable);
				}
				eventable.onEvent(evMsg);
			}
				break;

			case 5: // on new session
			{
				long ptrSes = eventQueue.getLong();
				String uri = readString(eventQueue);
				String srcIP = readString(eventQueue);

				synchronized (eventables) {
					eventable = eventables.get(id);
				}
				if (eventable == null) {
					LOG.warn(this.toString() + " eventable with id " + id + " was not found in map");
					break;
				}
				EventNewSession evNewSes = new EventNewSession(eventType, id, ptrSes, uri, srcIP);
				eventable.onEvent(evNewSes);
			}
				break;

			case 7: // on fd ready
			{
				/*
				 * int fd = eventQueue.getInt();
				 * int events = eventQueue.getInt();
				 */
				LOG.error("received FD Ready event - not handled");
			}
				break;

			default:
				LOG.error("received an unknown event " + eventType);
				// TODO: throw exception
		}
	}

	private String readString(ByteBuffer buf) {
		int len = buf.getInt();
		byte b[] = new byte[len];
		buf.get(b, 0, len);
		String s1 = new String(b, Charset.forName("US-ASCII"));
		return s1;
	}

	static private class DataFromC {
		long       ptrCtx;
		ByteBuffer eventQueue;

		DataFromC() {
			ptrCtx = 0;
			eventQueue = null;
		}
	}

	public boolean bindMsgPool(MsgPool msgPool) {
		if (getId() == 0) {
			LOG.error("no context opened on C side. can not bind msg pool");
			return false;
		}
		// the messages inside the pool must be added to hashmap, so that the appropraite msg can be tracked
		// once a request arrives
		List<Msg> msgArray = msgPool.getAllMsg();
		for (Msg msg : msgArray) {
			msgsPendingNewRequest.put(msg.getId(), msg);
		}
		return Bridge.bindMsgPool(msgPool.getId(), this.getId());
	}

	void releaseMsgBackToPool(Msg msg) {
		msg.resetPositions();
		this.msgsPendingNewRequest.put(msg.getId(), msg);
	}

	public void releaseMsgPool(MsgPool msgPool) {
		// TODO implement!
	}
}
